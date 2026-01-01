package com.example.final_project_114;

import com.example.final_project_114.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:watchstore.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
        }
        return connection;
    }

    private static void initializeTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "email TEXT," +
                    "is_admin INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS watches (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "brand TEXT NOT NULL," +
                    "price REAL NOT NULL," +
                    "description TEXT," +
                    "image_url TEXT," +
                    "stock INTEGER DEFAULT 0," +
                    "category TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS wishlist (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "watch_id INTEGER," +
                    "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(watch_id) REFERENCES watches(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS cart (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "watch_id INTEGER," +
                    "quantity INTEGER DEFAULT 1," +
                    "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(watch_id) REFERENCES watches(id)," +
                    "UNIQUE(user_id, watch_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS comments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "watch_id INTEGER," +
                    "comment_text TEXT NOT NULL," +
                    "rating INTEGER CHECK(rating >= 1 AND rating <= 5)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(watch_id) REFERENCES watches(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "total_amount REAL NOT NULL," +
                    "status TEXT DEFAULT 'Pending'," +
                    "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "order_id INTEGER NOT NULL," +
                    "watch_id INTEGER NOT NULL," +
                    "quantity INTEGER NOT NULL," +
                    "price REAL NOT NULL," +
                    "FOREIGN KEY(order_id) REFERENCES orders(id)," +
                    "FOREIGN KEY(watch_id) REFERENCES watches(id))");

            PreparedStatement checkAdmin = connection.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE username = 'admin'");
            ResultSet rs = checkAdmin.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = PasswordUtil.hashPassword("admin123");
                PreparedStatement insertAdmin = connection.prepareStatement(
                        "INSERT INTO users (username, password, email, is_admin) VALUES (?, ?, ?, 1)");
                insertAdmin.setString(1, "admin");
                insertAdmin.setString(2, hashedPassword);
                insertAdmin.setString(3, "admin@watchstore.com");
                insertAdmin.executeUpdate();
            }

            logger.info("Database initialized successfully!");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
}