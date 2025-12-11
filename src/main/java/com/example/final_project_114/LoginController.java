package com.example.final_project_114;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields", "error");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE username = ? AND password = ?")) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String email = rs.getString("email");
                boolean isAdmin = rs.getInt("is_admin") == 1;

                SessionManager.getInstance().setCurrentUser(userId, username, email, isAdmin);

                showMessage("Login successful! Welcome " + username, "success");

                if (isAdmin) {
                    loadScene("admin-dashboard.fxml", "Admin Dashboard");
                } else {
                    loadScene("user-dashboard.fxml", "Watch Store");
                }

            } else {
                showMessage("Invalid username or password", "error");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Database error: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            loadScene("register.fxml", "Create Account");
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error loading registration page", "error");
        }
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Error loading page: " + e.getMessage(), "error");
        } 
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        if (type.equals("error")) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else if (type.equals("success")) {
            messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }
}