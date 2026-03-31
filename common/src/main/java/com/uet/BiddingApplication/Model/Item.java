package com.uet.BiddingApplication.Model;

import java.time.LocalDateTime;

public class Item extends Entity {
    private String name;
    private String description;
    private String category;
    private String imageURL;
    private String sellerId;

    public Item(){};
    public Item(String id, LocalDateTime createdAt, String name, String description,
                String category, String imageURL, String sellerId) {
        super(id, createdAt);
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageURL = imageURL;
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}
