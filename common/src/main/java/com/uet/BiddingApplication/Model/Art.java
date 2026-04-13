package com.uet.BiddingApplication.Model;

import com.uet.BiddingApplication.Enum.Category;

import java.time.LocalDateTime;

public class Art extends Item{
    private String artistName;

    public Art(String id, LocalDateTime createdAt, String name, String description,
               Category category, String imageURL, String sellerId, String artistName) {
        super(id, createdAt, name, description, category, imageURL, sellerId);
        this.artistName = artistName;
    }

    public String getArtistName() {
        return artistName;
    }

    public Art() {
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    @Override
    public String getDisplayAttributes() {
        // Tự lấy biến String riêng của nó
        return "Tác giả: " + (this.artistName != null ? this.artistName : "Đang cập nhật");
    }
}
