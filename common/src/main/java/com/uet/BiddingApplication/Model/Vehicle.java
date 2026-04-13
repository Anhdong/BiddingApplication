package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.Category;

import java.time.LocalDateTime;

public class Vehicle extends Item{
    private String condition;

    public Vehicle() {
    }

    public Vehicle(String id, LocalDateTime createdAt, String name, String description,
                   Category category, String imageURL, String sellerId, String condition) {
        super(id, createdAt, name, description, category, imageURL, sellerId);
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
