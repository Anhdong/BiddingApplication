package com.uet.BiddingApplication.DTO.Request;

import com.uet.BiddingApplication.Enum.Category;

import java.io.Serializable;

public class ItemUpdateRequestDTO  {
    private String name;
    private String description;
    private Category category;
    private String oldImageURL;
    private byte[] imageBytes;
    private String imageExtension;
    private String attribute;

    public ItemUpdateRequestDTO() {
    }

    public ItemUpdateRequestDTO(String name, String description, Category category, String oldImageURL,
                                byte[] imageBytes, String imageExtension, String attribute) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.oldImageURL = oldImageURL;
        this.imageBytes = imageBytes;
        this.imageExtension = imageExtension;
        this.attribute = attribute;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getOldImageURL() {
        return oldImageURL;
    }

    public void setOldImageURL(String oldImageURL) {
        this.oldImageURL = oldImageURL;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public String getImageExtension() {
        return imageExtension;
    }

    public void setImageExtension(String imageExtension) {
        this.imageExtension = imageExtension;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
