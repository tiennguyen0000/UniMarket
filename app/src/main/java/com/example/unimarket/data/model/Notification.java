package com.example.unimarket.data.model;

public class Notification {
    private String id;
    private String user_id;
    private String content;
    private boolean is_read;
    private String created_at;

    public Notification() {
    }

    public Notification(String id, String user_id, String content, boolean is_read, String created_at) {
        this.id = id;
        this.user_id = user_id;
        this.content = content;
        this.is_read = is_read;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isRead() { return is_read; }
    public void setIs_read(boolean is_read) { this.is_read = is_read; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
