package com.example.final_project_114;

import com.example.final_project_114.util.PasswordUtil;
import com.example.final_project_114.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button registerButton;

    @FXML
    public void initialize() {
        confirmPasswordField.setOnAction(event -> handleRegister());
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(email) || 
            !ValidationUtil.isNotEmpty(password) || !ValidationUtil.isNotEmpty(confirmPassword)) {
            showMessage("Please fill in all fields", "error");
            return;
        }

        if (!ValidationUtil.isValidUsername(username)) {
            showMessage("Username must be 3-20 characters (letters, numbers, underscore only)", "error");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showMessage("Please enter a valid email address", "error");
            return;
        }

        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters long", "error");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Passwords do not match", "error");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email, is_admin) VALUES (?, ?, ?, 0)")) {

            String hashedPassword = PasswordUtil.hashPassword(password);
            
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);

            pstmt.executeUpdate();

            showMessage("Account created successfully! Redirecting to login...", "success");
            logger.info("New user registered: {}", username);

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::handleBackToLogin);
                } catch (InterruptedException e) {
                    logger.error("Thread interrupted", e);
                }
            }).start();

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                showMessage("Username already exists. Please choose another.", "error");
            } else {
                logger.error("Database error during registration", e);
                showMessage("Database error: " + e.getMessage(), "error");
            }
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Crown & Dial - Login");
            stage.show();
        } catch (IOException e) {
            logger.error("Error loading login page", e);
            showMessage("Error loading login page", "error");
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