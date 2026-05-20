package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemCreateDTO  {
    private String name;
    private String description;
    private Category category;
    private byte[] imageBytes;
    private String imageExtension;
    private BigDecimal startPrice;
    private BigDecimal bidStep;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String attribute;

    public ItemCreateDTO() {
    }


    public ItemCreateDTO(String name, String description, Category category, byte[] imageBytes,
                         String imageExtension, BigDecimal startPrice,BigDecimal bidStep, LocalDateTime startTime,
                         LocalDateTime endTime, String attribute) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageBytes = imageBytes;
        this.imageExtension = imageExtension;
        this.startPrice = startPrice;
        this.bidStep = bidStep;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public BigDecimal getBidStep() {
        return bidStep;
    }

    public void setBidStep(BigDecimal bidStep) {
        this.bidStep = bidStep;
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
