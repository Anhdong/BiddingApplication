package com.uet.BiddingApplication.Model;

import java.time.LocalDateTime;

public abstract class Entity {
    private String id;
    private LocalDateTime createdAt;

    public Entity() {}

    public Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
