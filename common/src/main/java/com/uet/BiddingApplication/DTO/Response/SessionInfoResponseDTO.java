package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.SessionStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SessionInfoResponseDTO  {
    private String sessionId;
    private String sellerName;
    private String itemName;
    private String description;
    private String imageUrl;
    private Category category;
    private String attribute;
    private BigDecimal startPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;

    public SessionInfoResponseDTO(String sessionId,String sellerName, String itemName, String description,
                                  String imageUrl, Category category, String attribute,
                                  BigDecimal startPrice, LocalDateTime startTime, LocalDateTime endTime,
                                  SessionStatus status) {
        this.sessionId = sessionId;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.attribute = attribute;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public SessionInfoResponseDTO() {
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
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
