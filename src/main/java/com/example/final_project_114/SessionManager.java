package com.example.final_project_114;

public class SessionManager {
    private static SessionManager instance;

    private int userId;
    private String username;
    private String email;
    private boolean isAdmin;
    private boolean isLoggedIn;

    private SessionManager() {
        this.isLoggedIn = false;
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(int userId, String username, String email, boolean isAdmin) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isLoggedIn = true;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void logout() {
        this.userId = 0;
        this.username = null;
        this.email = null;
        this.isAdmin = false;
        this.isLoggedIn = false;
    }
}