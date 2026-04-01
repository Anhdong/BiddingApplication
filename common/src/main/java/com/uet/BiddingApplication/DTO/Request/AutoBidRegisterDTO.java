package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;
import java.math.BigDecimal;

public class AutoBidRegisterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public AutoBidRegisterDTO() {
    }

    private String sessionId;
    private BigDecimal maxBid;
    private BigDecimal increment;

    public AutoBidRegisterDTO(String sessionId, BigDecimal maxBid, BigDecimal increment) {
        this.sessionId = sessionId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
    }

    public BigDecimal getIncrement() {
        return increment;
    }

    public void setIncrement(BigDecimal increment) {
        this.increment = increment;
    }
}
