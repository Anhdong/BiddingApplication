package com.uet.BiddingApplication.DTO.Packet;

import java.io.Serializable;

public class ResponsePacket<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int stausCode;
    private String message;
    private T payload;

    public ResponsePacket(int stausCode, String message, T payload) {
        this.stausCode = stausCode;
        this.message = message;
        this.payload = payload;
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
