package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;

public class ClientSession {


    private static ClientSession instance;

    private UserProfileDTO currentUser;

    private String currentToken;

    private ClientSession() {
    }

    public static synchronized ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
    }

    public void updateLocalSession(UserProfileDTO user, String token) {
        this.currentUser = user;
        this.currentToken = token;
    }

    public UserProfileDTO getCurrentUser() {
        return currentUser;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public void logout() {
        this.currentUser = null;
        this.currentToken = null;
    }
}