package com.example.final_project_114.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class Order {
    private final IntegerProperty id;
    private final IntegerProperty userId;
    private final StringProperty username;
    private final DoubleProperty totalAmount;
    private final StringProperty status;
    private final ObjectProperty<LocalDateTime> orderDate;

    public Order(int id, int userId, String username, double totalAmount, String status, LocalDateTime orderDate) {
        this.id = new SimpleIntegerProperty(id);
        this.userId = new SimpleIntegerProperty(userId);
        this.username = new SimpleStringProperty(username);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.status = new SimpleStringProperty(status);
        this.orderDate = new SimpleObjectProperty<>(orderDate);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public double getTotalAmount() { return totalAmount.get(); }
    public DoubleProperty totalAmountProperty() { return totalAmount; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }

    public LocalDateTime getOrderDate() { return orderDate.get(); }
    public ObjectProperty<LocalDateTime> orderDateProperty() { return orderDate; }
}
