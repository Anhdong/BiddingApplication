package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.BidType;

import java.math.BigDecimal;

public class BidRequestDTO  {
    private String sessionId;
    private String bidderName;
    private BigDecimal bidAmount;
    private BidType bidType;

    public BidRequestDTO(String sessionId,String bidderName,BigDecimal bidAmount, BidType bidType) {
        this.sessionId = sessionId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidType = bidType;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public BidRequestDTO() {
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
