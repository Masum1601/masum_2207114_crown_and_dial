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
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        passwordField.setOnAction(event -> handleLogin());
        passwordTextField.setOnAction(event -> handleLogin());
        
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void handleShowPassword() {
        if (showPasswordCheckBox.isSelected()) {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(password)) {
            showMessage("Please fill in all fields", "error");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                
                if (PasswordUtil.verifyPassword(password, storedPassword)) {
                    int userId = rs.getInt("id");
                    String email = rs.getString("email");
                    boolean isAdmin = rs.getInt("is_admin") == 1;

                    SessionManager.getInstance().setCurrentUser(userId, username, email, isAdmin);

                    showMessage("Login successful! Welcome " + username, "success");
                    logger.info("User logged in: {}", username);

                    if (isAdmin) {
                        loadScene("admin-dashboard.fxml", "Admin Dashboard");
                    } else {
                        loadScene("user-dashboard.fxml", "Watch Store");
                    }
                } else {
                    showMessage("Invalid username or password", "error");
                    logger.warn("Failed login attempt for user: {}", username);
                }
            } else {
                showMessage("Invalid username or password", "error");
                logger.warn("Failed login attempt for non-existent user: {}", username);
            }

        } catch (SQLException e) {
            logger.error("Database error during login", e);
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
            logger.error("Error loading scene: {}", fxmlFile, e);
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