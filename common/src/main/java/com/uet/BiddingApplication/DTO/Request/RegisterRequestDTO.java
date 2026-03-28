package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.RoleType;

import java.io.Serializable;

public class RegisterRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String email;
    private String phone;
    private RoleType role;
    private String specialAttribute;

    public RegisterRequestDTO(String username, String password, String email,
                              String phone, RoleType role, String specialAttribute) {
        this.username = username;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
