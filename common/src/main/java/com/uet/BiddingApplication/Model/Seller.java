package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.RoleType;

import java.time.LocalDateTime;

public class Seller extends User {
    private String bankAccount;

    public Seller() {
    }

    public Seller(String id, LocalDateTime createdAt, String username, String email,
                  String phone, String passwordHash, RoleType role, String bankAccount) {
        super(id, createdAt, username, email, phone, passwordHash, role);
        this.bankAccount = bankAccount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }
}
