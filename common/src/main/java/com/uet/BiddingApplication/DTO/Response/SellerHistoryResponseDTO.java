package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SellerHistoryResponseDTO implements Serializable {
    private String sessionId;
    private String itemName;
    private BigDecimal startPrice;
    private BigDecimal finalPrice; // Giá chốt cuối cùng
    private SessionStatus status;  // FINISHED, CANCELLED...
    private String winnerName;       // (Có thể null nếu ế)
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public SellerHistoryResponseDTO() {
    }

    public SellerHistoryResponseDTO(String sessionId, String itemName, BigDecimal startPrice,
                                    BigDecimal finalPrice, SessionStatus status, String winnerName,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.finalPrice = finalPrice;
        this.status = status;
        this.winnerName = winnerName;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}