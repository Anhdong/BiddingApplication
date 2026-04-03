package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RealtimeUpdateDTO  {
    private BidHistoryDTO lastBid;
    private LocalDateTime newEndTime; // Sẽ NULL nếu không có gia hạn, có giá trị nếu Anti-sniping kích hoạt

    public RealtimeUpdateDTO(BidHistoryDTO lastBid, LocalDateTime newEndTime) {
        this.lastBid = lastBid;
        this.newEndTime = newEndTime;
    }

    public RealtimeUpdateDTO() {
    }

    public BidHistoryDTO getLastBid() {
        return lastBid;
    }

    public void setLastBid(BidHistoryDTO lastBid) {
        this.lastBid = lastBid;
    }

    public LocalDateTime getNewEndTime() {
        return newEndTime;
    }

    public void setNewEndTime(LocalDateTime newEndTime) {
        this.newEndTime = newEndTime;
    }
}
