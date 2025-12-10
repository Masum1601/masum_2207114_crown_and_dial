package com.example.final_project_114;

import java.sql.*;

public class DatabaseManager {
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
                    "FOREIGN KEY(watch_id) REFERENCES watches(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS comments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "watch_id INTEGER," +
                    "comment_text TEXT NOT NULL," +
                    "rating INTEGER," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(watch_id) REFERENCES watches(id))");

            stmt.execute("INSERT OR IGNORE INTO users (username, password, email, is_admin) " +
                    "VALUES ('admin', 'admin123', 'admin@watchstore.com', 1)");

            System.out.println("Database initialized successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}