package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;

public class PasswordChangeRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String oldPassword;
    private String newPassword;

    public PasswordChangeRequestDTO(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public PasswordChangeRequestDTO() {
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
