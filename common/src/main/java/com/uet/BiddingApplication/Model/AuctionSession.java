package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionSession extends Entity{
    private String itemId;
    private String sellerId;
    private String winnerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal bidStep;

    public AuctionSession(String id, LocalDateTime createdAt, String itemId, String sellerId,
                          String winnerId, LocalDateTime startTime, LocalDateTime endTime,
                          SessionStatus status, BigDecimal startPrice, BigDecimal currentPrice,
                          BigDecimal bidStep) {
        super(id, createdAt);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.bidStep = bidStep;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
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

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getBidStep() {
        return bidStep;
    }

    public void setBidStep(BigDecimal bidStep) {
        this.bidStep = bidStep;
    }
}
