package com.example.final_project_114;

import com.example.final_project_114.model.CartItem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;

public class CartController {
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> nameColumn;
    @FXML private TableColumn<CartItem, String> brandColumn;
    @FXML private TableColumn<CartItem, Double> priceColumn;
    @FXML private TableColumn<CartItem, Integer> quantityColumn;
    @FXML private TableColumn<CartItem, Double> subtotalColumn;
    @FXML private TableColumn<CartItem, Void> actionsColumn;

    @FXML private Label totalItemsLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label userLabel;

    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        userLabel.setText("Hello, " + SessionManager.getInstance().getUsername());
        setupTableColumns();
        loadCartItems();
        updateTotals();
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("watchName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        priceColumn.setCellFactory(col -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });

        subtotalColumn.setCellFactory(col -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                setText(empty || subtotal == null ? null : String.format("$%.2f", subtotal));
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<CartItem, Void>() {
            private final Button increaseBtn = new Button("➕");
            private final Button decreaseBtn = new Button("➖");
            private final Button removeBtn = new Button("🗑️");
            private final HBox buttons = new HBox(5, increaseBtn, decreaseBtn, removeBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                increaseBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                decreaseBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");

                increaseBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    updateQuantity(item, item.getQuantity() + 1);
                });

                decreaseBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() > 1) {
                        updateQuantity(item, item.getQuantity() - 1);
                    }
                });

                removeBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    removeFromCart(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        cartTable.setItems(cartItems);
    }

    private void loadCartItems() {
        cartItems.clear();
        String query = "SELECT c.id, c.watch_id, c.quantity, w.name, w.brand, w.price, w.stock " +
                "FROM cart c JOIN watches w ON c.watch_id = w.id " +
                "WHERE c.user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, SessionManager.getInstance().getUserId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                cartItems.add(new CartItem(
                        rs.getInt("id"),
                        rs.getInt("watch_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getInt("stock")
                ));
            }

        } catch (SQLException e) {
            logger.error("Failed to load cart items", e);
            showAlert("Error", "Failed to load cart items", Alert.AlertType.ERROR);
        }
    }

    private void updateQuantity(CartItem item, int newQuantity) {
        if (newQuantity > item.getStock()) {
            showAlert("Stock Limit", "Only " + item.getStock() + " items available in stock", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE cart SET quantity = ? WHERE id = ?")) {

            pstmt.setInt(1, newQuantity);
            pstmt.setInt(2, item.getCartId());
            pstmt.executeUpdate();

            loadCartItems();
            updateTotals();

        } catch (SQLException e) {
            logger.error("Failed to update quantity", e);
            showAlert("Error", "Failed to update quantity", Alert.AlertType.ERROR);
        }
    }

    private void removeFromCart(CartItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Item");
        confirm.setHeaderText("Remove " + item.getWatchName() + " from cart?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM cart WHERE id = ?")) {

                    pstmt.setInt(1, item.getCartId());
                    pstmt.executeUpdate();

                    loadCartItems();
                    updateTotals();
                    showAlert("Success", "Item removed from cart", Alert.AlertType.INFORMATION);

                } catch (SQLException e) {
                    logger.error("Failed to remove item", e);
                    showAlert("Error", "Failed to remove item", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void updateTotals() {
        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        double totalPrice = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();

        totalItemsLabel.setText(String.valueOf(totalItems));
        totalPriceLabel.setText(String.format("$%.2f", totalPrice));
    }

    @FXML
    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            showAlert("Empty Cart", "Your cart is empty", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Checkout");
        confirm.setHeaderText("Complete your purchase?");
        confirm.setContentText("Total: " + totalPriceLabel.getText());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                processCheckout();
            }
        });
    }

    private void processCheckout() {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            double totalAmount = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();

            PreparedStatement insertOrder = conn.prepareStatement(
                    "INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            insertOrder.setInt(1, SessionManager.getInstance().getUserId());
            insertOrder.setDouble(2, totalAmount);
            insertOrder.setString(3, "Pending");
            insertOrder.executeUpdate();

            ResultSet generatedKeys = insertOrder.getGeneratedKeys();
            int orderId = 0;
            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
            }

            for (CartItem item : cartItems) {
                PreparedStatement insertOrderItem = conn.prepareStatement(
                        "INSERT INTO order_items (order_id, watch_id, quantity, price) VALUES (?, ?, ?, ?)");
                insertOrderItem.setInt(1, orderId);
                insertOrderItem.setInt(2, item.getWatchId());
                insertOrderItem.setInt(3, item.getQuantity());
                insertOrderItem.setDouble(4, item.getPrice());
                insertOrderItem.executeUpdate();

                PreparedStatement updateStock = conn.prepareStatement(
                        "UPDATE watches SET stock = stock - ? WHERE id = ?");
                updateStock.setInt(1, item.getQuantity());
                updateStock.setInt(2, item.getWatchId());
                updateStock.executeUpdate();
            }

            PreparedStatement clearCart = conn.prepareStatement(
                    "DELETE FROM cart WHERE user_id = ?");
            clearCart.setInt(1, SessionManager.getInstance().getUserId());
            clearCart.executeUpdate();

            conn.commit();
            logger.info("Order #{} created successfully for user {}", orderId, SessionManager.getInstance().getUserId());

            showAlert("Success", "Order #" + orderId + " placed successfully!\nTotal: $" + 
                    String.format("%.2f", totalAmount), Alert.AlertType.INFORMATION);
            loadCartItems();
            updateTotals();

        } catch (SQLException e) {
            logger.error("Checkout failed", e);
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                logger.error("Rollback failed", ex);
            }
            showAlert("Error", "Checkout failed: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }

    @FXML
    private void handleClearCart() {
        if (cartItems.isEmpty()) {
            showAlert("Empty Cart", "Your cart is already empty", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cart");
        confirm.setHeaderText("Remove all items from cart?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "DELETE FROM cart WHERE user_id = ?")) {

                    pstmt.setInt(1, SessionManager.getInstance().getUserId());
                    pstmt.executeUpdate();

                    loadCartItems();
                    updateTotals();
                    showAlert("Success", "Cart cleared", Alert.AlertType.INFORMATION);

                } catch (SQLException e) {
                    logger.error("Failed to clear cart", e);
                    showAlert("Error", "Failed to clear cart", Alert.AlertType.ERROR);
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