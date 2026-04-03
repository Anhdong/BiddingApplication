package com.uet.BiddingApplication.DTO.Response;

import java.io.Serializable;

public class AuthResponseDTO  {
    private String token;
    private UserProfileDTO userProfile;

    public AuthResponseDTO(String token, UserProfileDTO userProfile) {
        this.token = token;
        this.userProfile = userProfile;
    }

    public AuthResponseDTO() {
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
