package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;

public class RealtimeUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private BigDecimal newCurrentPrice;
    private String newWinnerName;

    public RealtimeUpdateDTO(String sessionId, BigDecimal newCurrentPrice, String newWinnerName) {
        this.sessionId = sessionId;
        this.newCurrentPrice = newCurrentPrice;
        this.newWinnerName = newWinnerName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getNewCurrentPrice() {
        return newCurrentPrice;
    }

    public void setNewCurrentPrice(BigDecimal newCurrentPrice) {
        this.newCurrentPrice = newCurrentPrice;
    }

    public String getNewWinnerName() {
        return newWinnerName;
    }

    public void setNewWinnerName(String newWinnerName) {
        this.newWinnerName = newWinnerName;
    }
}
