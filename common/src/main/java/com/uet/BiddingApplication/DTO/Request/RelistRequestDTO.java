package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RelistRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String sessionId;
    private BigDecimal newStartPrice;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;

    public RelistRequestDTO(String itemId,String sessionId, BigDecimal newStartPrice,
                            LocalDateTime newStartTime, LocalDateTime newEndTime) {
        this.itemId = itemId;
        this.sessionId = sessionId;
        this.newStartPrice = newStartPrice;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
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
