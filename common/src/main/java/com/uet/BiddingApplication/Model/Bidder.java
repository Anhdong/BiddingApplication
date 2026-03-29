package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.RoleType;

import java.time.LocalDateTime;

public class Bidder extends User {
    private String shippingAddress;

    public Bidder(String id, LocalDateTime createdAt, String username, String email, String phone,
                  String passwordHash, RoleType role, String shippingAddress) {
        super(id, createdAt, username, email, phone, passwordHash, role);
        this.shippingAddress = shippingAddress;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
