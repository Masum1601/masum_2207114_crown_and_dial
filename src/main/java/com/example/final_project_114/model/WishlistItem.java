package com.example.final_project_114.model;

import javafx.beans.property.*;

public class WishlistItem {
    private final IntegerProperty wishlistId;
    private final IntegerProperty watchId;
    private final StringProperty watchName;
    private final StringProperty brand;
    private final DoubleProperty price;
    private final StringProperty category;
    private final IntegerProperty stock;

    public WishlistItem(int wishlistId, int watchId, String watchName, String brand, double price, String category, int stock) {
        this.wishlistId = new SimpleIntegerProperty(wishlistId);
        this.watchId = new SimpleIntegerProperty(watchId);
        this.watchName = new SimpleStringProperty(watchName);
        this.brand = new SimpleStringProperty(brand);
        this.price = new SimpleDoubleProperty(price);
        this.category = new SimpleStringProperty(category);
        this.stock = new SimpleIntegerProperty(stock);
    }

    public int getWishlistId() { return wishlistId.get(); }
    public IntegerProperty wishlistIdProperty() { return wishlistId; }

    public int getWatchId() { return watchId.get(); }
    public IntegerProperty watchIdProperty() { return watchId; }

    public String getWatchName() { return watchName.get(); }
    public StringProperty watchNameProperty() { return watchName; }

    public String getBrand() { return brand.get(); }
    public StringProperty brandProperty() { return brand; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public int getStock() { return stock.get(); }
    public IntegerProperty stockProperty() { return stock; }
}
