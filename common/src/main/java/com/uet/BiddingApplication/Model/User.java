package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.RoleType;

import java.time.LocalDateTime;

public class User extends Entity{
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private RoleType role;
    private boolean isActive;

    public User(String id, LocalDateTime createdAt, String username, String email, String phone,
                String passwordHash, RoleType role) {
        super(id, createdAt);
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = true;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
