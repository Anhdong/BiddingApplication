package com.uet.BiddingApplication.DTO.Request;

public class AdminActionRequestDTO {
    private String targetId;
    private String actionReason;
    private String key;

    public AdminActionRequestDTO(String targetId, String actionReason, String key) {
        this.targetId = targetId;
        this.actionReason = actionReason;
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

    public String getActionReason() {
        return actionReason;
    }

    public void setActionReason(String actionReason) {
        this.actionReason = actionReason;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
