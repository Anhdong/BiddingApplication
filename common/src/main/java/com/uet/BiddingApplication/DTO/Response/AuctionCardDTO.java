package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionCardDTO  {
    private String itemId;
    private String sessionId;
    private String itemName;
    private String imageURL;
    private BigDecimal startPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;

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

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public AuctionCardDTO(String sessionId, String itemName, String imageURL, BigDecimal startPrice,
                          LocalDateTime startTime, LocalDateTime endTime, SessionStatus status) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.imageURL = imageURL;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
}
