package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private String category;

    public ItemCreateDTO() {
    }

    private byte[] imageBytes;
    private String imageExtension;
    private BigDecimal startPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String attribute;

    public ItemCreateDTO(String name, String description, String category, byte[] imageBytes,
                         String imageExtension, BigDecimal startPrice, LocalDateTime startTime,
                         LocalDateTime endTime, String attribute) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageBytes = imageBytes;
        this.imageExtension = imageExtension;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attribute = attribute;
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

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
}
