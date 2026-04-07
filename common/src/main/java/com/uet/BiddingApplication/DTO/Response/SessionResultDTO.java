package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.math.BigDecimal;

public class SessionResultDTO {
    private String sessionId;
    private BigDecimal finalPrice;
    private SessionStatus status;

    private String winnerId;

    public SessionResultDTO() {
    }

    public SessionResultDTO(String sessionId, BigDecimal finalPrice, SessionStatus status, String winnerId) {
        this.sessionId = sessionId;
        this.finalPrice = finalPrice;
        this.status = status;
        this.winnerId = winnerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }
}
