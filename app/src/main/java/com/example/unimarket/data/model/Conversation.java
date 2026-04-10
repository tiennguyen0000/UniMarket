package com.example.unimarket.data.model;

public class Conversation {
    private String id;
    private String created_at;

    public Conversation() {
    }

    public Conversation(String id, String created_at) {
        this.id = id;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
