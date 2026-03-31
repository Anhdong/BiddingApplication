package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.RoleType;

import java.time.LocalDateTime;

public class Admin extends User{
    private String otpSecretKey;

    public Admin(String id, LocalDateTime createdAt, String username, String email, String phone,
                 String passwordHash, RoleType role, String otpSecretKey) {
        super(id, createdAt, username, email, phone, passwordHash, role);
        this.otpSecretKey = otpSecretKey;
    }

    public Admin() {
    }

    public String getOtpSecretKey() {
        return otpSecretKey;
    }

    public void setOtpSecretKey(String otpSecretKey) {
        this.otpSecretKey = otpSecretKey;
    }
}
