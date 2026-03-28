package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;

public class AuthResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private UserProfileDTO userProfile;

    public AuthResponseDTO(String token, UserProfileDTO userProfile) {
        this.token = token;
        this.userProfile = userProfile;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserProfileDTO getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfileDTO userProfile) {
        this.userProfile = userProfile;
    }
}
