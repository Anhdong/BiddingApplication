package com.uet.BiddingApplication.DTO.Request;

public class AdminActionRequestDTO {
    private String targetId;
    private String key;

    public AdminActionRequestDTO(String targetId, String key) {
        this.targetId = targetId;
        this.key = key;
    }

    public AdminActionRequestDTO() {
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
