package com.uet.BiddingApplication.DTO.Request;

public class ItemTargetRequestDTO {
    private String itemId;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public ItemTargetRequestDTO(String itemId) {
        this.itemId = itemId;
    }
}
