package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Model.AutoBidSetting;

import java.math.BigDecimal;
import java.util.List;

public class AuctionRoomSyncDTO {
    private String sessionId;
    private String itemName;
    private String imageURL;
    private String description;
    private BigDecimal currentPrice;
    private BigDecimal bidStep;
    private AutoBidSetting autoBidSetting;
    private long remainingMillis; // Để chạy đồng hồ đếm ngược
    private List<BidHistoryDTO> history; // Để vẽ ngay lập tức cái table lúc mới vào
    private String highestBidderName; // Tên người đang giữ giá cao nhất hiện tại

    public AuctionRoomSyncDTO(String sessionId,String sessionName,String imageURL,String description ,BigDecimal currentPrice,BigDecimal bidStep,
                              AutoBidSetting autoBidSetting,long remainingMillis,
                              List<BidHistoryDTO> history, String highestBidderName) {
        this.sessionId = sessionId;
        this.itemName = sessionName;
        this.imageURL = imageURL;
        this.description = description;
        this.currentPrice = currentPrice;
        this.bidStep = bidStep;
        this.autoBidSetting = autoBidSetting;
        this.remainingMillis=remainingMillis;
        this.history = history;
        this.highestBidderName = highestBidderName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public AuctionRoomSyncDTO() {
    }

    public AutoBidSetting getAutoBidSetting() {
        return autoBidSetting;
    }

    public void setAutoBidSetting(AutoBidSetting autoBidSetting) {
        this.autoBidSetting = autoBidSetting;
    }

    public BigDecimal getBidStep() {
        return bidStep;
    }

    public void setBidStep(BigDecimal bidStep) {
        this.bidStep = bidStep;
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

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public void setRemainingMillis(long remainingMillis) {
        this.remainingMillis = remainingMillis;
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