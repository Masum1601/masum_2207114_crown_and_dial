package com.example.final_project_114.model;

import javafx.beans.property.*;

public class Watch {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty brand;
    private final DoubleProperty price;
    private final StringProperty description;
    private final IntegerProperty stock;
    private final StringProperty category;
    private final StringProperty imageUrl;

    public Watch(int id, String name, String brand, double price, String description, int stock, String category) {
        this(id, name, brand, price, description, stock, category, null);
    }

    public Watch(int id, String name, String brand, double price, String description, int stock, String category, String imageUrl) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.brand = new SimpleStringProperty(brand);
        this.price = new SimpleDoubleProperty(price);
        this.description = new SimpleStringProperty(description);
        this.stock = new SimpleIntegerProperty(stock);
        this.category = new SimpleStringProperty(category);
        this.imageUrl = new SimpleStringProperty(imageUrl);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getBrand() { return brand.get(); }
    public StringProperty brandProperty() { return brand; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public int getStock() { return stock.get(); }
    public IntegerProperty stockProperty() { return stock; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public String getImageUrl() { return imageUrl.get(); }
    public StringProperty imageUrlProperty() { return imageUrl; }
}
