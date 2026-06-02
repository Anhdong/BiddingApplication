package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidderHistoryResponseDTO  {
    private String sessionId;
    private String itemName;
    private String winnerName;
    private BigDecimal myHighestBid;
    private BigDecimal finalPrice;
    private SessionStatus status;
    private LocalDateTime time;

    public BidderHistoryResponseDTO(String sessionId, String itemName, String winnerName,
                                    BigDecimal myHighestBid, BigDecimal finalPrice, SessionStatus status,
                                    LocalDateTime time) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.winnerName = winnerName;
        this.myHighestBid = myHighestBid;
        this.finalPrice = finalPrice;
        this.status = status;
        this.time = time;
    }

    public BidderHistoryResponseDTO() {
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

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
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
