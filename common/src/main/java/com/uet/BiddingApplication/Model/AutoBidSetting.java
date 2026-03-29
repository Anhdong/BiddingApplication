package com.uet.BiddingApplication.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

public class AutoBidSetting extends Entity implements Comparable<AutoBidSetting>{
    private String bidderId;
    private String sessionId;
    private BigDecimal maxBid;
    private BigDecimal increment;

    public AutoBidSetting(String id, LocalDateTime createdAt, String bidderId, String sessionId,
                          BigDecimal maxBid, BigDecimal increment) {
        super(id, createdAt);
        this.bidderId = bidderId;
        this.sessionId = sessionId;
        this.maxBid = maxBid;
        this.increment = increment;
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

    @Override
    public int compareTo(AutoBidSetting other) {
        return Comparator.comparing(AutoBidSetting::getCreatedAt)
                .thenComparing(AutoBidSetting::getId) // Tiêu chí phụ nếu trùng thời gian
                .compare(this, other);
    }
}
