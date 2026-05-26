package com.uet.BiddingApplication.DTO.Request;

import java.math.BigDecimal;

public class AutoBidRegisterDTO {

    public AutoBidRegisterDTO() {
    }

    private String sessionId;
    private String bidderName;
    private BigDecimal maxBid;
    private BigDecimal increment;

    public AutoBidRegisterDTO(String sessionId,String bidderName, BigDecimal maxBid, BigDecimal increment) {
        this.sessionId = sessionId;
        this.bidderName = bidderName;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
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
