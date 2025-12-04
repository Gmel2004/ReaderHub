package com.example.readerhub.api.models;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private UserData user;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserData getUser() { return user; }

    public static class UserData {
        private int id;
        private String username;
        private String email;

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }
}