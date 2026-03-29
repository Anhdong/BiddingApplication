package com.uet.BiddingApplication.Model;

import java.time.LocalDateTime;

public class Art extends Item{
    private String artistName;

    public Art(String id, LocalDateTime createdAt, String name, String description,
               String category, String imageURL, String sellerId, String artistName) {
        super(id, createdAt, name, description, category, imageURL, sellerId);
        this.artistName = artistName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}
