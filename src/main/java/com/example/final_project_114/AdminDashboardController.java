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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class AdminDashboardController {

    @FXML private Label adminLabel;
    @FXML private Label totalWatchesLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalRevenueLabel;

    @FXML private TableView<Watch> watchTable;
    @FXML private TableColumn<Watch, Integer> idColumn;
    @FXML private TableColumn<Watch, String> nameColumn;
    @FXML private TableColumn<Watch, String> brandColumn;
    @FXML private TableColumn<Watch, Double> priceColumn;
    @FXML private TableColumn<Watch, Integer> stockColumn;
    @FXML private TableColumn<Watch, String> categoryColumn;
    @FXML private TableColumn<Watch, Void> actionsColumn;

    private ObservableList<Watch> watchList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        adminLabel.setText("Hello, " + SessionManager.getInstance().getUsername());

        setupTableColumns();
        loadStatistics();
        loadWatches();
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
                 ResultSet rs = stmt.executeQuery("SELECT SUM(price * stock) as total FROM watches")) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    totalRevenueLabel.setText(String.format("$%.2f", total));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
                        rs.getString("category")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
                    e.printStackTrace();
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
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Luxury", "Sport", "Classic", "Smart");
        categoryBox.setValue(isEdit ? watch.getCategory() : null);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Brand:"), 0, 1);
        grid.add(brandField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("Category:"), 0, 4);
        grid.add(categoryBox, 1, 4);
        grid.add(new Label("Description:"), 0, 5);
        grid.add(descField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    return new Watch(
                            isEdit ? watch.getId() : 0,
                            nameField.getText(),
                            brandField.getText(),
                            Double.parseDouble(priceField.getText()),
                            descField.getText(),
                            Integer.parseInt(stockField.getText()),
                            categoryBox.getValue()
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
                     "INSERT INTO watches (name, brand, price, description, stock, category) VALUES (?, ?, ?, ?, ?, ?)")) {

            pstmt.setString(1, watch.getName());
            pstmt.setString(2, watch.getBrand());
            pstmt.setDouble(3, watch.getPrice());
            pstmt.setString(4, watch.getDescription());
            pstmt.setInt(5, watch.getStock());
            pstmt.setString(6, watch.getCategory());

            pstmt.executeUpdate();
            showAlert("Success", "Watch added successfully!", Alert.AlertType.INFORMATION);
            loadWatches();
            loadStatistics();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to add watch: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateWatchInDB(Watch watch) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE watches SET name=?, brand=?, price=?, description=?, stock=?, category=? WHERE id=?")) {

            pstmt.setString(1, watch.getName());
            pstmt.setString(2, watch.getBrand());
            pstmt.setDouble(3, watch.getPrice());
            pstmt.setString(4, watch.getDescription());
            pstmt.setInt(5, watch.getStock());
            pstmt.setString(6, watch.getCategory());
            pstmt.setInt(7, watch.getId());

            pstmt.executeUpdate();
            showAlert("Success", "Watch updated successfully!", Alert.AlertType.INFORMATION);
            loadWatches();
            loadStatistics();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update watch", Alert.AlertType.ERROR);
        }
    }
    public static class Watch {
        private final IntegerProperty id;
        private final StringProperty name;
        private final StringProperty brand;
        private final DoubleProperty price;
        private final StringProperty description;
        private final IntegerProperty stock;
        private final StringProperty category;

        public Watch(int id, String name, String brand, double price, String description, int stock, String category) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.brand = new SimpleStringProperty(brand);
            this.price = new SimpleDoubleProperty(price);
            this.description = new SimpleStringProperty(description);
            this.stock = new SimpleIntegerProperty(stock);
            this.category = new SimpleStringProperty(category);
        }

        public int getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getBrand() { return brand.get(); }
        public double getPrice() { return price.get(); }
        public String getDescription() { return description.get(); }
        public int getStock() { return stock.get(); }
        public String getCategory() { return category.get(); }
    }
}
