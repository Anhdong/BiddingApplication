package com.uet.BiddingApplication.DTO.Response;

import com.uet.BiddingApplication.Enum.RoleType;

import java.io.Serializable;

public class UserProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String id;
    private String email;
    private String phone;
    private RoleType role;
    private String specialAttribute;

    public UserProfileDTO(String username, String id, String email, String phone,
                          RoleType role, String specialAttribute) {
        this.username = username;
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.specialAttribute = specialAttribute;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public String getSpecialAttribute() {
        return specialAttribute;
    }

    public void setSpecialAttribute(String specialAttribute) {
        this.specialAttribute = specialAttribute;
    }
}
