package com.example.final_project_114.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class Review {
    private final IntegerProperty id;
    private final IntegerProperty userId;
    private final StringProperty username;
    private final IntegerProperty watchId;
    private final StringProperty commentText;
    private final IntegerProperty rating;
    private final ObjectProperty<LocalDateTime> createdAt;

    public Review(int id, int userId, String username, int watchId, String commentText, int rating, LocalDateTime createdAt) {
        this.id = new SimpleIntegerProperty(id);
        this.userId = new SimpleIntegerProperty(userId);
        this.username = new SimpleStringProperty(username);
        this.watchId = new SimpleIntegerProperty(watchId);
        this.commentText = new SimpleStringProperty(commentText);
        this.rating = new SimpleIntegerProperty(rating);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public int getWatchId() { return watchId.get(); }
    public IntegerProperty watchIdProperty() { return watchId; }

    public String getCommentText() { return commentText.get(); }
    public StringProperty commentTextProperty() { return commentText; }

    public int getRating() { return rating.get(); }
    public IntegerProperty ratingProperty() { return rating; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}
