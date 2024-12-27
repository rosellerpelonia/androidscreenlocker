package com.example.androidscreenlocker;

public class ParentUser {
    private String name;
    private String email;
    private String role;

    // Empty constructor required for Firebase
    public ParentUser() { }

    public ParentUser(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

