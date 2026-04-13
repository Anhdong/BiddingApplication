package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;
import java.util.Objects;

public class AdminActionRequestDTO {
    private String targetId;
    private String actionReason;
    private String otpCode;

    public AdminActionRequestDTO(String targetId, String actionReason, String otpCode) {
        this.targetId = targetId;
        this.actionReason = actionReason;
        this.otpCode = otpCode;
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

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
