package com.example.final_project_114.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class User {
    private final IntegerProperty id;
    private final StringProperty username;
    private final StringProperty email;
    private final BooleanProperty isAdmin;
    private final ObjectProperty<LocalDateTime> createdAt;

    public User(int id, String username, String email, boolean isAdmin, LocalDateTime createdAt) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.email = new SimpleStringProperty(email);
        this.isAdmin = new SimpleBooleanProperty(isAdmin);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }
    public void setUsername(String username) { this.username.set(username); }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }
    public void setEmail(String email) { this.email.set(email); }

    public boolean isAdmin() { return isAdmin.get(); }
    public BooleanProperty isAdminProperty() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin.set(admin); }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
}
