package com.example.final_project_114;

import com.example.final_project_114.model.Order;
import com.example.final_project_114.model.Watch;
import com.example.final_project_114.util.ValidationUtil;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @FXML private Label adminLabel;
    @FXML private Label totalWatchesLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalOrdersLabel;

    @FXML private TableView<Watch> watchTable;
    @FXML private TableColumn<Watch, Integer> idColumn;
    @FXML private TableColumn<Watch, String> nameColumn;
    @FXML private TableColumn<Watch, String> brandColumn;
    @FXML private TableColumn<Watch, Double> priceColumn;
    @FXML private TableColumn<Watch, Integer> stockColumn;
    @FXML private TableColumn<Watch, String> categoryColumn;
    @FXML private TableColumn<Watch, Void> actionsColumn;

    @FXML private TabPane mainTabPane;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, String> customerColumn;
    @FXML private TableColumn<Order, LocalDateTime> orderDateColumn;
    @FXML private TableColumn<Order, Double> orderTotalColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private TableColumn<Order, Void> orderActionsColumn;

    private ObservableList<Watch> watchList = FXCollections.observableArrayList();
    private ObservableList<Order> ordersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        adminLabel.setText("Hello, " + SessionManager.getInstance().getUsername());

        setupTableColumns();
        setupOrdersTableColumns();
        loadStatistics();
        loadWatches();
        loadOrders();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        priceColumn.setCellFactory(col -> new TableCell<Watch, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<Watch, Void>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button deleteBtn = new Button("🗑️ Delete");
            private final HBox buttons = new HBox(5, editBtn, deleteBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

                editBtn.setOnAction(e -> handleEditWatch(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDeleteWatch(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        watchTable.setItems(watchList);
    }

    private void loadStatistics() {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM watches")) {
                if (rs.next()) totalWatchesLabel.setText(String.valueOf(rs.getInt("count")));
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users WHERE is_admin = 0")) {
                if (rs.next()) totalUsersLabel.setText(String.valueOf(rs.getInt("count")));
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT SUM(total_amount) as total FROM orders")) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    totalRevenueLabel.setText(String.format("$%.2f", total));
                }
            }
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM orders")) {
                if (rs.next()) {
                    if (totalOrdersLabel != null) {
                        totalOrdersLabel.setText(String.valueOf(rs.getInt("count")));
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to load statistics", e);
        }
    }

    private void loadWatches() {
        watchList.clear();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM watches ORDER BY id DESC")) {

            while (rs.next()) {
                watchList.add(new Watch(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getString("description"),
                        rs.getInt("stock"),
                        rs.getString("category"),
                        rs.getString("image_url")
                ));
            }

        } catch (SQLException e) {
            logger.error("Failed to load watches", e);
            showAlert("Error", "Failed to load watches", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAddWatch() {
        Dialog<Watch> dialog = createWatchDialog(null);
        dialog.showAndWait().ifPresent(this::insertWatchIntoDB);
    }

    private void handleEditWatch(Watch watch) {
        Dialog<Watch> dialog = createWatchDialog(watch);
        dialog.showAndWait().ifPresent(this::updateWatchInDB);
    }

    private void handleDeleteWatch(Watch watch) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + watch.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM watches WHERE id = ?")) {

                    pstmt.setInt(1, watch.getId());
                    pstmt.executeUpdate();

                    showAlert("Success", "Watch deleted successfully!", Alert.AlertType.INFORMATION);
                    loadWatches();
                    loadStatistics();

                } catch (SQLException e) {
                    logger.error("Failed to delete watch", e);
                    showAlert("Error", "Failed to delete watch", Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        loadScene("login.fxml", "Login - Crown & Dial");
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) adminLabel.getScene().getWindow();
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
    private Dialog<Watch> createWatchDialog(Watch watch) {
        boolean isEdit = watch != null;
        Dialog<Watch> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Watch" : "Add New Watch");
        dialog.setHeaderText(isEdit ? "Update watch details" : "Enter watch details");

        ButtonType confirmButtonType = new ButtonType(isEdit ? "Update" : "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(isEdit ? watch.getName() : "");
        TextField brandField = new TextField(isEdit ? watch.getBrand() : "");
        TextField priceField = new TextField(isEdit ? String.valueOf(watch.getPrice()) : "");
        TextField stockField = new TextField(isEdit ? String.valueOf(watch.getStock()) : "");
        TextArea descField = new TextArea(isEdit ? watch.getDescription() : "");
        descField.setPrefRowCount(3);
        TextField categoryField = new TextField(isEdit ? watch.getCategory() : "");
        categoryField.setPromptText("e.g., Luxury, Sport, Classic, Smart");
        TextField imageUrlField = new TextField(isEdit ? (watch.getImageUrl() != null ? watch.getImageUrl() : "") : "");
        imageUrlField.setPromptText("https://example.com/image.jpg");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Brand:"), 0, 1);
        grid.add(brandField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("Category:"), 0, 4);
        grid.add(categoryField, 1, 4);
        grid.add(new Label("Image URL:"), 0, 5);
        grid.add(imageUrlField, 1, 5);
        grid.add(new Label("Description:"), 0, 6);
        grid.add(descField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    String category = categoryField.getText().trim();
                    if (category.isEmpty()) {
                        showAlert("Error", "Please enter a category", Alert.AlertType.ERROR);
                        return null;
                    }
                    String imageUrl = imageUrlField.getText().trim();
                    return new Watch(
                            isEdit ? watch.getId() : 0,
                            nameField.getText(),
                            brandField.getText(),
                            Double.parseDouble(priceField.getText()),
                            descField.getText(),
                            Integer.parseInt(stockField.getText()),
                            category,
                            imageUrl.isEmpty() ? null : imageUrl
                    );
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter valid numbers", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private void insertWatchIntoDB(Watch watch) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO watches (name, brand, price, description, stock, category, image_url) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

            pstmt.setString(1, watch.getName());
            pstmt.setString(2, watch.getBrand());
            pstmt.setDouble(3, watch.getPrice());
            pstmt.setString(4, watch.getDescription());
            pstmt.setInt(5, watch.getStock());
            pstmt.setString(6, watch.getCategory());
            pstmt.setString(7, watch.getImageUrl());

            logger.info("Inserting watch with image URL: {}", watch.getImageUrl());
            pstmt.executeUpdate();
            showAlert("Success", "Watch added successfully!", Alert.AlertType.INFORMATION);
            loadWatches();
            loadStatistics();

        } catch (SQLException e) {
            logger.error("Failed to add watch", e);
            showAlert("Error", "Failed to add watch: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateWatchInDB(Watch watch) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE watches SET name=?, brand=?, price=?, description=?, stock=?, category=?, image_url=? WHERE id=?")) {

            pstmt.setString(1, watch.getName());
            pstmt.setString(2, watch.getBrand());
            pstmt.setDouble(3, watch.getPrice());
            pstmt.setString(4, watch.getDescription());
            pstmt.setInt(5, watch.getStock());
            pstmt.setString(6, watch.getCategory());
            pstmt.setString(7, watch.getImageUrl());
            pstmt.setInt(8, watch.getId());

            logger.info("Updating watch ID {} with image URL: {}", watch.getId(), watch.getImageUrl());
            pstmt.executeUpdate();
            showAlert("Success", "Watch updated successfully!", Alert.AlertType.INFORMATION);
            loadWatches();
            loadStatistics();

        } catch (SQLException e) {
            logger.error("Failed to update watch", e);
            showAlert("Error", "Failed to update watch", Alert.AlertType.ERROR);
        }
    }
    
    private void setupOrdersTableColumns() {
        if (ordersTable == null) return;
        
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        orderTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        orderDateColumn.setCellFactory(col -> new TableCell<Order, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        orderTotalColumn.setCellFactory(col -> new TableCell<Order, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("$%.2f", total));
            }
        });

        orderStatusColumn.setCellFactory(col -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equals("Completed")) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (status.equals("Pending")) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else if (status.equals("Cancelled")) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        orderActionsColumn.setCellFactory(col -> new TableCell<Order, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button completeBtn = new Button("✅");
            private final Button cancelBtn = new Button("❌");
            private final HBox buttons = new HBox(5, viewBtn, completeBtn, cancelBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                completeBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");

                viewBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    viewOrderDetails(order);
                });

                completeBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    updateOrderStatus(order, "Completed");
                });

                cancelBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    updateOrderStatus(order, "Cancelled");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Order order = getTableView().getItems().get(getIndex());
                    completeBtn.setDisable(order.getStatus().equals("Completed"));
                    cancelBtn.setDisable(order.getStatus().equals("Cancelled"));
                    setGraphic(buttons);
                }
            }
        });

        ordersTable.setItems(ordersList);
    }

    private void loadOrders() {
        if (ordersTable == null) return;
        
        ordersList.clear();
        String query = "SELECT o.*, u.username FROM orders o " +
                      "JOIN users u ON o.user_id = u.id " +
                      "ORDER BY o.order_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                ordersList.add(new Order(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getDouble("total_amount"),
                        rs.getString("status"),
                        rs.getTimestamp("order_date").toLocalDateTime()
                ));
            }

        } catch (SQLException e) {
            logger.error("Failed to load orders", e);
            showAlert("Error", "Failed to load orders", Alert.AlertType.ERROR);
        }
    }

    private void viewOrderDetails(Order order) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Order #" + order.getId() + " Details");
        
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        Label customerInfo = new Label("Customer: " + order.getUsername());
        customerInfo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label dateInfo = new Label("Date: " + order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        dateInfo.setStyle("-fx-font-size: 14px;");
        
        Label statusInfo = new Label("Status: " + order.getStatus());
        statusInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label totalInfo = new Label("Total: $" + String.format("%.2f", order.getTotalAmount()));
        totalInfo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        TableView<OrderItemData> itemsTable = new TableView<>();
        TableColumn<OrderItemData, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(data -> data.getValue().watchName);
        nameCol.setPrefWidth(200);
        
        TableColumn<OrderItemData, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(data -> data.getValue().quantity.asObject());
        qtyCol.setPrefWidth(80);
        
        TableColumn<OrderItemData, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> data.getValue().price.asObject());
        priceCol.setCellFactory(col -> new TableCell<OrderItemData, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });
        priceCol.setPrefWidth(100);

        TableColumn<OrderItemData, Double> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(data -> {
            double subtotal = data.getValue().price.get() * data.getValue().quantity.get();
            return new javafx.beans.property.SimpleDoubleProperty(subtotal).asObject();
        });
        subtotalCol.setCellFactory(col -> new TableCell<OrderItemData, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                setText(empty || subtotal == null ? null : String.format("$%.2f", subtotal));
            }
        });
        subtotalCol.setPrefWidth(100);

        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, subtotalCol);
        itemsTable.setPrefHeight(250);
        loadOrderItems(order.getId(), itemsTable);

        content.getChildren().addAll(customerInfo, dateInfo, statusInfo, new Label("Items:"), itemsTable, totalInfo);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void loadOrderItems(int orderId, TableView<OrderItemData> table) {
        String query = "SELECT w.name, oi.quantity, oi.price " +
                      "FROM order_items oi " +
                      "JOIN watches w ON oi.watch_id = w.id " +
                      "WHERE oi.order_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                table.getItems().add(new OrderItemData(
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price")
                ));
            }

        } catch (SQLException e) {
            logger.error("Failed to load order items", e);
        }
    }

    private void updateOrderStatus(Order order, String newStatus) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Update Order Status");
        confirm.setHeaderText("Change order #" + order.getId() + " to " + newStatus + "?");
        confirm.setContentText("This will update the order status.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "UPDATE orders SET status = ? WHERE id = ?")) {

                    pstmt.setString(1, newStatus);
                    pstmt.setInt(2, order.getId());
                    pstmt.executeUpdate();

                    showAlert("Success", "Order status updated to " + newStatus, Alert.AlertType.INFORMATION);
                    loadOrders();
                    loadStatistics();

                } catch (SQLException e) {
                    logger.error("Failed to update order status", e);
                    showAlert("Error", "Failed to update order status", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private static class OrderItemData {
        private final javafx.beans.property.StringProperty watchName;
        private final javafx.beans.property.IntegerProperty quantity;
        private final javafx.beans.property.DoubleProperty price;

        public OrderItemData(String watchName, int quantity, double price) {
            this.watchName = new javafx.beans.property.SimpleStringProperty(watchName);
            this.quantity = new javafx.beans.property.SimpleIntegerProperty(quantity);
            this.price = new javafx.beans.property.SimpleDoubleProperty(price);
        }
    }
}
