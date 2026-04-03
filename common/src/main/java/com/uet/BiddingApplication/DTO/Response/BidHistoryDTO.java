package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidHistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String bidderName;
    private BigDecimal bidAmount;
    private LocalDateTime time;
    private String sessionId;

    public BidHistoryDTO() {
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BidHistoryDTO(String bidderName, BigDecimal bidAmount, LocalDateTime time, String sessionId) {
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.time = time;
        this.sessionId = sessionId;
    }
}
