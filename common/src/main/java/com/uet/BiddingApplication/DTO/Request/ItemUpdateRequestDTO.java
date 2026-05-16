package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.Category;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemUpdateRequestDTO  {
    private String itemId;
    private String name;
    private String description;
    private Category category;
    private String oldImageURL;
    private byte[] imageBytes;
    private String imageExtension;
    private String attribute;
    private BigDecimal startPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ItemUpdateRequestDTO() {
    }

    public ItemUpdateRequestDTO(String itemId, String name, String description, Category category,
                                String oldImageURL, byte[] imageBytes, String imageExtension,
                                String attribute, BigDecimal startPrice, LocalDateTime startTime,
                                LocalDateTime endTime) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.oldImageURL = oldImageURL;
        this.imageBytes = imageBytes;
        this.imageExtension = imageExtension;
        this.attribute = attribute;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getItemId() {
        return itemId;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getOldImageURL() {
        return oldImageURL;
    }

    public void setOldImageURL(String oldImageURL) {
        this.oldImageURL = oldImageURL;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public String getImageExtension() {
        return imageExtension;
    }

    public void setImageExtension(String imageExtension) {
        this.imageExtension = imageExtension;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
