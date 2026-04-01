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
}
