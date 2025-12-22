package com.example.readerhub.models;

/**
 * Модель для сущности Пользователь
 */
public class User {
    private int id;
    private String username;
    private String email;
    private String token;

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}