package com.uet.BiddingApplication.DTO.Packet;

import com.uet.BiddingApplication.Enum.ActionType;

public class ResponsePacket<T> {


    private ActionType action;
    private int statusCode;
    private String message;
    private T payload;

    public ResponsePacket(ActionType action, int statusCode, String message, T payload) {
        this.action = action;
        this.statusCode = statusCode;
        this.message = message;
        this.payload = payload;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
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