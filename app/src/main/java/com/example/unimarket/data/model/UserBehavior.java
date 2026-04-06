package com.example.unimarket.data.model;

public class UserBehavior {
    private Long id;
    private Long userId;
    private Long productId;
    private String action;
    private String createdAt;

    public UserBehavior() {
    }

    public UserBehavior(Long id, Long userId, Long productId, String action, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.action = action;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
