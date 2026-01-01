package com.example.final_project_114;

import com.example.final_project_114.model.Order;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderHistoryController {
    private static final Logger logger = LoggerFactory.getLogger(OrderHistoryController.class);

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, LocalDateTime> orderDateColumn;
    @FXML private TableColumn<Order, Double> totalColumn;
    @FXML private TableColumn<Order, String> statusColumn;
    @FXML private TableColumn<Order, Void> actionsColumn;

    @FXML private Label userLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalSpentLabel;

    private ObservableList<Order> ordersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        userLabel.setText("Hello, " + SessionManager.getInstance().getUsername());
        setupTableColumns();
        loadOrders();
        updateStatistics();
    }

    private void setupTableColumns() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
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

        totalColumn.setCellFactory(col -> new TableCell<Order, Double>() {
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

        statusColumn.setCellFactory(col -> new TableCell<Order, String>() {
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

        actionsColumn.setCellFactory(col -> new TableCell<Order, Void>() {
            private final Button viewBtn = new Button("📄 Details");

            {
                viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                viewBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    showOrderDetails(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        ordersTable.setItems(ordersList);
    }

    private void loadOrders() {
        ordersList.clear();
        String query = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, SessionManager.getInstance().getUserId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ordersList.add(new Order(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        SessionManager.getInstance().getUsername(),
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

    private void updateStatistics() {
        totalOrdersLabel.setText(String.valueOf(ordersList.size()));
        double totalSpent = ordersList.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
        totalSpentLabel.setText(String.format("$%.2f", totalSpent));
    }

    private void showOrderDetails(Order order) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Order #" + order.getId() + " Details");
        dialog.setHeaderText("Order Date: " + 
                order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label statusLabel = new Label("Status: " + order.getStatus());
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label totalLabel = new Label("Total: $" + String.format("%.2f", order.getTotalAmount()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        TableView<OrderItemData> itemsTable = new TableView<>();
        TableColumn<OrderItemData, String> nameCol = new TableColumn<>("Watch");
        nameCol.setCellValueFactory(data -> data.getValue().watchName);
        
        TableColumn<OrderItemData, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(data -> data.getValue().quantity.asObject());
        
        TableColumn<OrderItemData, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> data.getValue().price.asObject());
        priceCol.setCellFactory(col -> new TableCell<OrderItemData, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });

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

        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, subtotalCol);
        itemsTable.setPrefHeight(200);

        loadOrderItems(order.getId(), itemsTable);

        content.getChildren().addAll(statusLabel, totalLabel, new Label("Items:"), itemsTable);

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

    @FXML
    private void handleBack() {
        loadScene("user-dashboard.fxml", "Watch Store");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        loadScene("login.fxml", "Login - Watch Store");
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
