package com.uet.BiddingApplication.DTO.Packet;

import com.uet.BiddingApplication.Enum.ActionType;

import java.io.Serializable;

public class RequestPacket<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private ActionType action;
    private String userId;
    private String token;
    private T payload;

    public RequestPacket(ActionType action, String userId, String token, T payload) {
        this.action = action;
        this.userId = userId;
        this.token = token;
        this.payload = payload;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
