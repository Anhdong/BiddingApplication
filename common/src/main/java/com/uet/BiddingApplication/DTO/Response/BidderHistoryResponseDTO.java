package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidderHistoryResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String itemName;
    private String WinnerId;
    private BigDecimal myHighestBid;
    private BigDecimal finalPrice;
    private SessionStatus status;
    private LocalDateTime time;

    public BidderHistoryResponseDTO(String sessionId, String itemName, String winnerId,
                                    BigDecimal myHighestBid, BigDecimal finalPrice, SessionStatus status,
                                    LocalDateTime time) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        WinnerId = winnerId;
        this.myHighestBid = myHighestBid;
        this.finalPrice = finalPrice;
        this.status = status;
        this.time = time;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public SessionStatus getStatus() {
        return status;
    }
    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getWinnerId() {
        return WinnerId;
    }

    public void setWinnerId(String winnerId) {
        WinnerId = winnerId;
    }

    public BigDecimal getMyHighestBid() {
        return myHighestBid;
    }

    public void setMyHighestBid(BigDecimal myHighestBid) {
        this.myHighestBid = myHighestBid;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }
}
