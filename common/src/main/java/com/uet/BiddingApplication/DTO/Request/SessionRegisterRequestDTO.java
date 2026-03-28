package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;

public class SessionRegisterRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;

    public SessionRegisterRequestDTO(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
