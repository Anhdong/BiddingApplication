package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.BidType;

import java.io.Serializable;
import java.math.BigDecimal;

public class BidRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private BigDecimal bidAmount;
    private BidType bidType;

    public BidRequestDTO(String sessionId, BigDecimal bidAmount, BidType bidType) {
        this.sessionId = sessionId;
        this.bidAmount = bidAmount;
        this.bidType = bidType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public BidType getBidType() {
        return bidType;
    }

    public void setBidType(BidType bidType) {
        this.bidType = bidType;
    }
}
