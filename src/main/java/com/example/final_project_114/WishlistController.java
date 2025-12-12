package com.example.final_project_114;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class WishlistController {

    @FXML private TableView<WishlistItem> wishlistTable;
    @FXML private TableColumn<WishlistItem, String> nameColumn;
    @FXML private TableColumn<WishlistItem, String> brandColumn;
    @FXML private TableColumn<WishlistItem, Double> priceColumn;
    @FXML private TableColumn<WishlistItem, String> categoryColumn;
    @FXML private TableColumn<WishlistItem, Integer> stockColumn;
    @FXML private TableColumn<WishlistItem, Void> actionsColumn;

    @FXML private Label totalItemsLabel;
    @FXML private Label userLabel;

    private ObservableList<WishlistItem> wishlistItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        userLabel.setText("Hello, " + SessionManager.getInstance().getUsername());
        setupTableColumns();
        loadWishlistItems();
        updateTotals();
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("watchName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));

        priceColumn.setCellFactory(col -> new TableCell<WishlistItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });

        stockColumn.setCellFactory(col -> new TableCell<WishlistItem, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (!empty && stock != null) {
                    setText(String.valueOf(stock));
                    if (stock == 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (stock < 5) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                } else {
                    setText(null);
                    setStyle("");
                }
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<WishlistItem, Void>() {
            private final Button addToCartBtn = new Button("🛒 Add to Cart");
            private final Button removeBtn = new Button("🗑️ Remove");
            private final HBox buttons = new HBox(5, addToCartBtn, removeBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                addToCartBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");

                addToCartBtn.setOnAction(e -> {
                    WishlistItem item = getTableView().getItems().get(getIndex());
                    moveToCart(item);
                });

                removeBtn.setOnAction(e -> {
                    WishlistItem item = getTableView().getItems().get(getIndex());
                    removeFromWishlist(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    WishlistItem wishlistItem = getTableView().getItems().get(getIndex());
                    addToCartBtn.setDisable(wishlistItem.getStock() == 0);
                    setGraphic(buttons);
                } else {
                    setGraphic(null);
                }
            }
        });

        wishlistTable.setItems(wishlistItems);
    }

    private void loadWishlistItems() {
        wishlistItems.clear();
        String query = "SELECT wl.id, wl.watch_id, w.name, w.brand, w.price, w.category, w.stock " +
                "FROM wishlist wl JOIN watches w ON wl.watch_id = w.id " +
                "WHERE wl.user_id = ? ORDER BY wl.added_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, SessionManager.getInstance().getUserId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                wishlistItems.add(new WishlistItem(
                        rs.getInt("id"),
                        rs.getInt("watch_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getString("category"),
                        rs.getInt("stock")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load wishlist items", Alert.AlertType.ERROR);
        }
    }

    private void moveToCart(WishlistItem item) {
        if (item.getStock() == 0) {
            showAlert("Out of Stock", item.getWatchName() + " is currently out of stock", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id, quantity FROM cart WHERE user_id = ? AND watch_id = ?");
            checkStmt.setInt(1, SessionManager.getInstance().getUserId());
            checkStmt.setInt(2, item.getWatchId());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int currentQty = rs.getInt("quantity");
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE cart SET quantity = ? WHERE user_id = ? AND watch_id = ?");
                updateStmt.setInt(1, currentQty + 1);
                updateStmt.setInt(2, SessionManager.getInstance().getUserId());
                updateStmt.setInt(3, item.getWatchId());
                updateStmt.executeUpdate();
            } else {
                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO cart (user_id, watch_id, quantity) VALUES (?, ?, 1)");
                insertStmt.setInt(1, SessionManager.getInstance().getUserId());
                insertStmt.setInt(2, item.getWatchId());
                insertStmt.executeUpdate();
            }

            PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM wishlist WHERE id = ?");
            deleteStmt.setInt(1, item.getWishlistId());
            deleteStmt.executeUpdate();

            loadWishlistItems();
            updateTotals();
            showAlert("Success", item.getWatchName() + " moved to cart!", Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to move item to cart", Alert.AlertType.ERROR);
        }
    }

    private void removeFromWishlist(WishlistItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Item");
        confirm.setHeaderText("Remove " + item.getWatchName() + " from wishlist?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "DELETE FROM wishlist WHERE id = ?")) {

                    pstmt.setInt(1, item.getWishlistId());
                    pstmt.executeUpdate();

                    loadWishlistItems();
                    updateTotals();
                    showAlert("Success", "Item removed from wishlist", Alert.AlertType.INFORMATION);

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to remove item", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void updateTotals() {
        totalItemsLabel.setText(String.valueOf(wishlistItems.size()));
    }

    @FXML
    private void handleClearWishlist() {
        if (wishlistItems.isEmpty()) {
            showAlert("Empty Wishlist", "Your wishlist is already empty", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Wishlist");
        confirm.setHeaderText("Remove all items from wishlist?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "DELETE FROM wishlist WHERE user_id = ?")) {

                    pstmt.setInt(1, SessionManager.getInstance().getUserId());
                    pstmt.executeUpdate();

                    loadWishlistItems();
                    updateTotals();
                    showAlert("Success", "Wishlist cleared", Alert.AlertType.INFORMATION);

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to clear wishlist", Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleContinueShopping() {
        loadScene("user-dashboard.fxml", "Watch Store");
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
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class WishlistItem {
        private final IntegerProperty wishlistId;
        private final IntegerProperty watchId;
        private final StringProperty watchName;
        private final StringProperty brand;
        private final DoubleProperty price;
        private final StringProperty category;
        private final IntegerProperty stock;

        public WishlistItem(int wishlistId, int watchId, String watchName, String brand,
                            double price, String category, int stock) {
            this.wishlistId = new SimpleIntegerProperty(wishlistId);
            this.watchId = new SimpleIntegerProperty(watchId);
            this.watchName = new SimpleStringProperty(watchName);
            this.brand = new SimpleStringProperty(brand);
            this.price = new SimpleDoubleProperty(price);
            this.category = new SimpleStringProperty(category);
            this.stock = new SimpleIntegerProperty(stock);
        }

        public int getWishlistId() { return wishlistId.get(); }
        public int getWatchId() { return watchId.get(); }
        public String getWatchName() { return watchName.get(); }
        public String getBrand() { return brand.get(); }
        public double getPrice() { return price.get(); }
        public String getCategory() { return category.get(); }
        public int getStock() { return stock.get(); }
    }
}