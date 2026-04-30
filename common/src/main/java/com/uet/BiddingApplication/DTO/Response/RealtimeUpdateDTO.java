package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RealtimeUpdateDTO  {
    private BidHistoryDTO lastBid;
    private long remainingMillis; // Sẽ NULL nếu không có gia hạn, có giá trị nếu Anti-sniping kích hoạt

    public RealtimeUpdateDTO(BidHistoryDTO lastBid, long remainingMillis) {
        this.lastBid = lastBid;
        this.remainingMillis = remainingMillis;
    }

    public RealtimeUpdateDTO() {
    }

    public BidHistoryDTO getLastBid() {
        return lastBid;
    }

    public void setLastBid(BidHistoryDTO lastBid) {
        this.lastBid = lastBid;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public void setRemainingMillis(long remainingMillis) {
        this.remainingMillis = remainingMillis;
    }
}
