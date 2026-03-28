package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.RoleType;

import java.io.Serializable;

public class ProfileUpdateRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String phone;
    private String specialAttribute;

    public ProfileUpdateRequestDTO(String username, String phone, String specialAttribute) {
        this.username = username;
        this.phone = phone;
        this.specialAttribute = specialAttribute;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSpecialAttribute() {
        return specialAttribute;
    }

    public void setSpecialAttribute(String specialAttribute) {
        this.specialAttribute = specialAttribute;
    }
}
