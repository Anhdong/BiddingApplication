package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionRoomSyncDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String imageURL;
    private BigDecimal currentPrice;
    private LocalDateTime endTime; // Để chạy đồng hồ đếm ngược
    private List<BidHistoryDTO> history; // Để vẽ ngay lập tức cái table lúc mới vào
    private String highestBidderName; // Tên người đang giữ giá cao nhất hiện tại

    public AuctionRoomSyncDTO(String sessionId,String imageURL ,BigDecimal currentPrice, LocalDateTime endTime,
                              List<BidHistoryDTO> history, String highestBidderName) {
        this.sessionId = sessionId;
        this.imageURL = imageURL;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.history = history;
        this.highestBidderName = highestBidderName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<BidHistoryDTO> getHistory() {
        return history;
    }

    public void setHistory(List<BidHistoryDTO> history) {
        this.history = history;
    }

    public String getHighestBidderName() {
        return highestBidderName;
    }

    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = highestBidderName;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}