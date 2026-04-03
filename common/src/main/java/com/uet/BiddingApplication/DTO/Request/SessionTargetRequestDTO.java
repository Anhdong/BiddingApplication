package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;

public class SessionTargetRequestDTO  {
    private String sessionId;

    public SessionTargetRequestDTO(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
