package com.uet.BiddingApplication.Model;

import java.time.LocalDateTime;

public class SessionRegistration extends Entity{
    private String bidderId;
    private String sessionId;

    public SessionRegistration() {
    }

    public SessionRegistration(String id, LocalDateTime createdAt, String bidderId, String sessionId) {
        super(id, createdAt);
        this.bidderId = bidderId;
        this.sessionId = sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
