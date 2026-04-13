package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.Category;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private Category category;
    private String imageURL;
    private String sellerId;

    public Item(){};
    public Item(String id, LocalDateTime createdAt, String name, String description,
                Category category, String imageURL, String sellerId) {
        super(id, createdAt!=null ? createdAt:LocalDateTime.now());
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
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

    // Phương thức trừu tượng để lấy thuộc tính đặc thù
    public abstract String getDisplayAttributes();
}
