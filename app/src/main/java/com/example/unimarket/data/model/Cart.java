package com.example.unimarket.data.model;

public class Cart {
    private Long id;
    private Long userId;
    private String createdAt;

    public Cart() {
    }

    public Cart(Long id, Long userId, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
