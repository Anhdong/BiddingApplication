package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.RoleType;

import java.time.LocalDateTime;

public class Admin extends User{
    private String SecretKey;

    public Admin(String id, LocalDateTime createdAt, String username, String email, String phone,
                 String passwordHash, RoleType role, String SecretKey) {
        super(id, createdAt, username, email, phone, passwordHash, role);
        this.SecretKey = SecretKey;
    }

    public Admin() {
    }

    public String getSecretKey() {
        return SecretKey;
    }

    public void setSecretKey(String secretKey) {
        this.SecretKey = secretKey;
    }
}
