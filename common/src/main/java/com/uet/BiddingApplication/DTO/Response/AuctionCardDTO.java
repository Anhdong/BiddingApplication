package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionCardDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String itemName;
    private String imageURL;
    private BigDecimal startPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public AuctionCardDTO() {
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

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
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

    public AuctionCardDTO(String sessionId, String itemName, String imageURL, BigDecimal startPrice,
                          LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.imageURL = imageURL;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
