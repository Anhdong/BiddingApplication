package com.uet.BiddingApplication.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RelistRequestDTO {

    private String itemId;
    private String sessionId;
    private BigDecimal newStartPrice;
    private BigDecimal newBidStep;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;

    public RelistRequestDTO(String itemId,String sessionId, BigDecimal newStartPrice,BigDecimal newBidStep,
                            LocalDateTime newStartTime, LocalDateTime newEndTime) {
        this.itemId = itemId;
        this.sessionId = sessionId;
        this.newStartPrice = newStartPrice;
        this.newBidStep = newBidStep;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
    }

    public RelistRequestDTO() {
    }

    public BigDecimal getNewBidStep() {
        return newBidStep;
    }

    public void setNewBidStep(BigDecimal newBidStep) {
        this.newBidStep = newBidStep;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getNewStartPrice() {
        return newStartPrice;
    }

    public void setNewStartPrice(BigDecimal newStartPrice) {
        this.newStartPrice = newStartPrice;
    }

    public LocalDateTime getNewStartTime() {
        return newStartTime;
    }

    public void setNewStartTime(LocalDateTime newStartTime) {
        this.newStartTime = newStartTime;
    }

    public LocalDateTime getNewEndTime() {
        return newEndTime;
    }

    public void setNewEndTime(LocalDateTime newEndTime) {
        this.newEndTime = newEndTime;
    }
}
