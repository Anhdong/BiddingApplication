package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.BidType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction extends Entity{
    private String bidderId;
    private String sessionId;
    private BigDecimal bidAmount;
    private BidType bidType;

    public BidTransaction() {
    }

    public BidTransaction(String bidderId, String sessionId,
                          BigDecimal bidAmount, BidType bidType) {
        super(UUID.randomUUID().toString(),LocalDateTime.now());
        this.bidderId = bidderId;
        this.sessionId = sessionId;
        this.bidAmount = bidAmount;
        this.bidType = bidType;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
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
