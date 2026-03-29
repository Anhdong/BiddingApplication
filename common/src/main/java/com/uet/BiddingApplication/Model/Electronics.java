package com.uet.BiddingApplication.Model;

import java.time.LocalDateTime;

public class Electronics extends Item{
    private int warrantyMonths;

    public Electronics(String id, LocalDateTime createdAt, String name, String description,
                       String category, String imageURL, String sellerId, int warrantyMonths) {
        super(id, createdAt, name, description, category, imageURL, sellerId);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
