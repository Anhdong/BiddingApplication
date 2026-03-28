package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SessionInfoResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String itemName;
    private String description;
    private String imageUrl;
    private String category;
    private String attribute;
    private BigDecimal startPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public SessionInfoResponseDTO(String sessionId, String itemName, String description,
                                  String imageUrl, String category, String attribute,
                                  BigDecimal startPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.attribute = attribute;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
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
}
