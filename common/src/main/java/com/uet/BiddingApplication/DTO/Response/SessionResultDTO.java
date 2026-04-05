package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.SessionStatus;

import java.math.BigDecimal;

public class SessionResultDTO {
    private String sessionId;
    private BigDecimal finalPrice;
    private SessionStatus status;

    private String winnerId;
}
