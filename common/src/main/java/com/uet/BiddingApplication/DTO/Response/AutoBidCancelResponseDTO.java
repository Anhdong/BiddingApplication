package com.uet.BiddingApplication.DTO.Response;

public class AutoBidCancelResponseDTO {
    private String bidderId;
    private String sessionID;

    public AutoBidCancelResponseDTO() {
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public AutoBidCancelResponseDTO(String bidderId, String sessionID) {
        this.bidderId = bidderId;
        this.sessionID = sessionID;
    }
}
