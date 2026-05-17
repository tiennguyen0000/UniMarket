package com.example.unimarket.data.model;

import com.google.firebase.firestore.PropertyName;

public class Notification {
    private String id;
    private String user_id;
    private String title;
    private String content;
    private String type;
    private String target_id;
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

    public Notification(String id, String user_id, String title, String content, String type,
                        String target_id, boolean is_read, String created_at) {
        this.id = id;
        this.user_id = user_id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.target_id = target_id;
        this.is_read = is_read;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTarget_id() { return target_id; }
    public void setTarget_id(String target_id) { this.target_id = target_id; }
    @PropertyName("is_read")
    public boolean isRead() { return is_read; }
    @PropertyName("is_read")
    public void setIs_read(boolean is_read) { this.is_read = is_read; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
