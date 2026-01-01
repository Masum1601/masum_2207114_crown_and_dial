package com.example.final_project_114;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;

public class WatchDetailsController {
    private static final Logger logger = LoggerFactory.getLogger(WatchDetailsController.class);

    @FXML private ImageView watchImageView;
    @FXML private Label watchNameLabel;
    @FXML private Label watchBrandLabel;
    @FXML private Label watchPriceLabel;
    @FXML private Label watchCategoryLabel;
    @FXML private Label watchStockLabel;
    @FXML private TextArea watchDescriptionArea;
    @FXML private Spinner<Integer> quantitySpinner;

    private int watchId;
    private String watchName;
    private double watchPrice;

    public void setWatchId(int watchId) {
        this.watchId = watchId;
        loadWatchDetails();
    }

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);
        if (quantitySpinner != null) {
            quantitySpinner.setValueFactory(valueFactory);
        }
    }

    private void loadWatchDetails() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM watches WHERE id = ?")) {

            pstmt.setInt(1, watchId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                watchName = rs.getString("name");
                watchPrice = rs.getDouble("price");
                String brand = rs.getString("brand");
                String category = rs.getString("category");
                String description = rs.getString("description");
                int stock = rs.getInt("stock");
                String imageUrl = rs.getString("image_url");

                watchNameLabel.setText(watchName);
                watchBrandLabel.setText("Brand: " + brand);
                watchPriceLabel.setText(String.format("$%.2f", watchPrice));
                watchCategoryLabel.setText("Category: " + category);
                watchStockLabel.setText(stock > 0 ? "In Stock (" + stock + " available)" : "Out of Stock");
                watchDescriptionArea.setText(description);

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    try {
                        Image image = new Image(imageUrl, 400, 400, true, true, true);
                        watchImageView.setImage(image);
                        
                        image.errorProperty().addListener((obs, oldError, newError) -> {
                            if (newError) {
                                logger.error("Failed to load image: {}", imageUrl);
                            }
                        });
                    } catch (Exception e) {
                        logger.error("Error loading image", e);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to load watch details", e);
            showAlert("Error", "Failed to load watch details", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAddToCart() {
        int quantity = quantitySpinner.getValue();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id, quantity FROM cart WHERE user_id = ? AND watch_id = ?");
            checkStmt.setInt(1, SessionManager.getInstance().getUserId());
            checkStmt.setInt(2, watchId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int currentQty = rs.getInt("quantity");
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE cart SET quantity = ? WHERE user_id = ? AND watch_id = ?");
                updateStmt.setInt(1, currentQty + quantity);
                updateStmt.setInt(2, SessionManager.getInstance().getUserId());
                updateStmt.setInt(3, watchId);
                updateStmt.executeUpdate();
            } else {
                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO cart (user_id, watch_id, quantity) VALUES (?, ?, ?)");
                insertStmt.setInt(1, SessionManager.getInstance().getUserId());
                insertStmt.setInt(2, watchId);
                insertStmt.setInt(3, quantity);
                insertStmt.executeUpdate();
            }

            showAlert("Success", quantity + "x " + watchName + " added to cart!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            logger.error("Failed to add to cart", e);
            showAlert("Error", "Failed to add to cart", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAddToWishlist() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO wishlist (user_id, watch_id) VALUES (?, ?)")) {

            pstmt.setInt(1, SessionManager.getInstance().getUserId());
            pstmt.setInt(2, watchId);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                showAlert("Success", watchName + " added to wishlist!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Info", "Already in wishlist", Alert.AlertType.INFORMATION);
            }

        } catch (SQLException e) {
            logger.error("Failed to add to wishlist", e);
            showAlert("Error", "Failed to add to wishlist", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("user-dashboard.fxml"));
            Stage stage = (Stage) watchNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("User Dashboard - Crown & Dial");
        } catch (IOException e) {
            logger.error("Failed to load user dashboard", e);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
