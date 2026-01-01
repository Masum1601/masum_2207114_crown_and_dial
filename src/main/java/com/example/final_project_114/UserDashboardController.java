package com.example.final_project_114;

import com.example.final_project_114.model.Watch;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(UserDashboardController.class);

    @FXML private FlowPane watchContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Label userLabel;
    @FXML private Button cartButton;

    private String currentFilter = "ALL";
    private int cartItemCount = 0;

    @FXML
    public void initialize() {
        userLabel.setText("Hello, " + SessionManager.getInstance().getUsername());

        sortComboBox.getItems().addAll("Price: Low to High", "Price: High to Low", "Name: A-Z", "Brand: A-Z");

        loadWatches();
        updateCartCount();
    }

    private void loadWatches() {
        watchContainer.getChildren().clear();

        StringBuilder query = new StringBuilder("SELECT * FROM watches WHERE stock > 0");
        List<Object> params = new ArrayList<>();

        if (!currentFilter.equals("ALL")) {
            query.append(" AND category = ?");
            params.add(currentFilter);
        }

        String sortBy = sortComboBox.getValue();
        if (sortBy != null) {
            if (sortBy.equals("Price: Low to High")) query.append(" ORDER BY price ASC");
            else if (sortBy.equals("Price: High to Low")) query.append(" ORDER BY price DESC");
            else if (sortBy.equals("Name: A-Z")) query.append(" ORDER BY name ASC");
            else if (sortBy.equals("Brand: A-Z")) query.append(" ORDER BY brand ASC");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String imageUrl = rs.getString("image_url");
                logger.info("Loading watch: {} with image URL: {}", rs.getString("name"), imageUrl);
                watchContainer.getChildren().add(createWatchCard(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getString("description"),
                        rs.getInt("stock"),
                        imageUrl
                ));
            }

        } catch (SQLException e) {
            logger.error("Failed to load watches", e);
            showAlert("Error", "Failed to load watches: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox createWatchCard(int id, String name, String brand, double price, String desc, int stock, String imageUrl) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); -fx-cursor: hand;");
        card.setPrefWidth(250);
        card.setPrefHeight(350);

        VBox imageContainer = new VBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPrefHeight(150);
        imageContainer.setMinHeight(150);
        imageContainer.setStyle("-fx-background-color: #f8f9fa;");
        
        logger.info("Creating card for watch: {} with imageUrl: {}", name, imageUrl);
        
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            logger.info("Attempting to load image from: {}", imageUrl);
            ImageView imageView = new ImageView();
            imageView.setFitWidth(200);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            Image image = new Image(imageUrl, 200, 150, true, true, true);
            
            image.errorProperty().addListener((obs, oldError, newError) -> {
                if (newError) {
                    logger.error("Failed to load image from URL: {}", imageUrl);
                    imageContainer.getChildren().clear();
                    Label iconLabel = new Label("⌚");
                    iconLabel.setStyle("-fx-font-size: 60px;");
                    imageContainer.getChildren().add(iconLabel);
                }
            });
            
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                logger.info("Image loading progress for {}: {}", name, newProgress);
            });
            
            imageView.setImage(image);
            imageContainer.getChildren().add(imageView);
        } else {
            logger.info("No image URL for watch: {}", name);
            Label iconLabel = new Label("⌚");
            iconLabel.setStyle("-fx-font-size: 60px;");
            imageContainer.getChildren().add(iconLabel);
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        Label brandLabel = new Label(brand);
        brandLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label priceLabel = new Label("$" + String.format("%.2f", price));
        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        Label stockLabel = new Label("Stock: " + stock);
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(Pos.CENTER);

        Button addToCartBtn = new Button("🛒 Add");
        addToCartBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 5;");
        addToCartBtn.setOnAction(e -> addToCart(id, name));

        Button wishlistBtn = new Button("❤️");
        wishlistBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 5;");
        wishlistBtn.setOnAction(e -> addToWishlist(id, name));

        Button viewBtn = new Button("👁️ View");
        viewBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 5;");
        viewBtn.setOnAction(e -> viewWatchDetails(id));

        buttonBox.getChildren().addAll(addToCartBtn, wishlistBtn, viewBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imageContainer, nameLabel, brandLabel, priceLabel, stockLabel, spacer, buttonBox);

        return card;
    }

    private void addToCart(int watchId, String watchName) {
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
                updateStmt.setInt(1, currentQty + 1);
                updateStmt.setInt(2, SessionManager.getInstance().getUserId());
                updateStmt.setInt(3, watchId);
                updateStmt.executeUpdate();
            } else {
                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO cart (user_id, watch_id, quantity) VALUES (?, ?, 1)");
                insertStmt.setInt(1, SessionManager.getInstance().getUserId());
                insertStmt.setInt(2, watchId);
                insertStmt.executeUpdate();
            }

            updateCartCount();
            showAlert("Success", watchName + " added to cart!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            logger.error("Failed to add to cart", e);
            showAlert("Error", "Failed to add to cart", Alert.AlertType.ERROR);
        }
    }

    private void addToWishlist(int watchId, String watchName) {
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

    private void viewWatchDetails(int watchId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("watch-details.fxml"));
            Parent root = loader.load();
            
            WatchDetailsController controller = loader.getController();
            controller.setWatchId(watchId);
            
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Watch Details - Crown & Dial");
        } catch (IOException e) {
            logger.error("Failed to load watch details view", e);
            showAlert("Error", "Failed to load watch details", Alert.AlertType.ERROR);
        }
    }

    private void updateCartCount() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT COALESCE(SUM(quantity), 0) as total FROM cart WHERE user_id = ?")) {

            pstmt.setInt(1, SessionManager.getInstance().getUserId());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                cartItemCount = rs.getInt("total");
                cartButton.setText("🛒 Cart (" + cartItemCount + ")");
            }

        } catch (SQLException e) {
            logger.error("Failed to update cart count", e);
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            loadWatches();
            return;
        }

        watchContainer.getChildren().clear();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM watches WHERE stock > 0 AND " +
                             "(LOWER(name) LIKE ? OR LOWER(brand) LIKE ? OR LOWER(description) LIKE ?)")) {

            String pattern = "%" + searchTerm + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                watchContainer.getChildren().add(createWatchCard(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getString("description"),
                        rs.getInt("stock"),
                        rs.getString("image_url")
                ));
            }

        } catch (SQLException e) {
            logger.error("Failed to search watches", e);
        }
    }

    @FXML private void handleSort() { loadWatches(); }
    @FXML private void filterAll() { currentFilter = "ALL"; loadWatches(); }
    @FXML private void filterLuxury() { currentFilter = "Luxury"; loadWatches(); }
    @FXML private void filterSport() { currentFilter = "Sport"; loadWatches(); }
    @FXML private void filterClassic() { currentFilter = "Classic"; loadWatches(); }
    @FXML private void filterSmart() { currentFilter = "Smart"; loadWatches(); }

    @FXML
    private void handleCart() {
        loadScene("cart.fxml", "Shopping Cart - Crown & Dial");
    }

    @FXML
    private void handleWishlist() {
        loadScene("wishlist.fxml", "My Wishlist - Crown & Dial");
    }

    @FXML
    private void handleOrders() {
        loadScene("order-history.fxml", "Order History - Crown & Dial");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        loadScene("login.fxml", "Login - Crown & Dial");
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            logger.error("Failed to load scene: {}", fxmlFile, e);
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