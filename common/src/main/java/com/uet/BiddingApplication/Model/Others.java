package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.Category;

import java.time.LocalDateTime;

public class Others extends Item{
    public Others() {
    }

    public Others(String id, LocalDateTime createdAt, String name, String description,
                  Category category, String imageURL, String sellerId) {
        super(id, createdAt, name, description, category, imageURL, sellerId);
    }
}
