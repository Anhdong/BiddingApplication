package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;

public class JoinSessionRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;

    public JoinSessionRequestDTO(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
