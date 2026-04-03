package com.uet.BiddingApplication.DTO.Request;

import java.io.Serializable;

public class SessionFilterRequestDTO  {
    private String category;       // Ví dụ: "Electronics", "Arts", hoặc "ALL"
    private String timeSortOption; // Ví dụ: "ENDING_SOON", "NEWEST", "OLDEST"


    public SessionFilterRequestDTO(String category, String timeSortOption) {
        this.category = category;
        this.timeSortOption = timeSortOption;
    }

    public SessionFilterRequestDTO() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTimeSortOption() {
        return timeSortOption;
    }

    public void setTimeSortOption(String timeSortOption) {
        this.timeSortOption = timeSortOption;
    }
}