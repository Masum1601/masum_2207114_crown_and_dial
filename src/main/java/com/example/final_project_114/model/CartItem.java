package com.example.final_project_114.model;

import javafx.beans.property.*;

public class CartItem {
    private final IntegerProperty cartId;
    private final IntegerProperty watchId;
    private final StringProperty watchName;
    private final StringProperty brand;
    private final DoubleProperty price;
    private final IntegerProperty quantity;
    private final IntegerProperty stock;
    private final DoubleProperty subtotal;

    public CartItem(int cartId, int watchId, String watchName, String brand, double price, int quantity, int stock) {
        this.cartId = new SimpleIntegerProperty(cartId);
        this.watchId = new SimpleIntegerProperty(watchId);
        this.watchName = new SimpleStringProperty(watchName);
        this.brand = new SimpleStringProperty(brand);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.stock = new SimpleIntegerProperty(stock);
        this.subtotal = new SimpleDoubleProperty(price * quantity);
    }

    public int getCartId() { return cartId.get(); }
    public IntegerProperty cartIdProperty() { return cartId; }

    public int getWatchId() { return watchId.get(); }
    public IntegerProperty watchIdProperty() { return watchId; }

    public String getWatchName() { return watchName.get(); }
    public StringProperty watchNameProperty() { return watchName; }

    public String getBrand() { return brand.get(); }
    public StringProperty brandProperty() { return brand; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public IntegerProperty quantityProperty() { return quantity; }

    public int getStock() { return stock.get(); }
    public IntegerProperty stockProperty() { return stock; }

    public double getSubtotal() { return subtotal.get(); }
    public DoubleProperty subtotalProperty() { return subtotal; }
}
