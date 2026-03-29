package com.uet.BiddingApplication.Model;

import java.time.LocalDateTime;

public class Others extends Item{
    public Others(String id, LocalDateTime createdAt, String name, String description,
                  String category, String imageURL, String sellerId) {
        super(id, createdAt, name, description, category, imageURL, sellerId);
    }
}
