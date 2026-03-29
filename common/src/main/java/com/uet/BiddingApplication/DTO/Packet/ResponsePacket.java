package com.uet.BiddingApplication.DTO.Packet;

import com.uet.BiddingApplication.Enum.ActionType;

import java.io.Serializable;

public class ResponsePacket<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private ActionType action;
    private int stausCode;
    private String message;
    private T payload;

    public ResponsePacket(ActionType action,int stausCode, String message, T payload) {
        this.action = action;
        this.stausCode = stausCode;
        this.message = message;
        this.payload = payload;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public int getStausCode() {
        return stausCode;
    }

    public void setStausCode(int stausCode) {
        this.stausCode = stausCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
