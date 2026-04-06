package com.example.unimarket.data.model;

public class Conversation {
    private Long id;
    private String createdAt;

    public Conversation() {
    }

    public Conversation(Long id, String createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
