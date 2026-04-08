package com.example.unimarket.data.model;

public class Notification {
    private Long id;
    private Long userId;
    private String content;
    private boolean read;
    private String createdAt;

    public Notification() {
    }

    public Notification(Long id, Long userId, String content, boolean read, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.read = read;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
