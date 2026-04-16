package com.uet.BiddingApplication.DTO.Response;

import java.time.LocalDateTime;

public class SessionTargetDTO {
        private String sessionId;
        private  LocalDateTime startTime;
        private LocalDateTime endTime ;// Để client tính toán thời gian chạy của Progress Bar

    public SessionTargetDTO() {
    }

    public SessionTargetDTO(String sessionId, LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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