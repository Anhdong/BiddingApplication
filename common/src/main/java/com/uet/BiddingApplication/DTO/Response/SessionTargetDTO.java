package com.uet.BiddingApplication.DTO.Response;

import java.time.LocalDateTime;

public class SessionTargetDTO {
        private String sessionId;
        private long remainingMillis;

    public SessionTargetDTO() {
    }

    public SessionTargetDTO(String sessionId,long remainingMillis) {
        this.sessionId = sessionId;
        this.remainingMillis = remainingMillis;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public void setRemainingMillis(long remainingMillis) {
        this.remainingMillis = remainingMillis;
    }
}