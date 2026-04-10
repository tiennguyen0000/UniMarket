package com.example.unimarket.data.model;

public class Cart {
    private String id;
    private String user_id;
    private String created_at;

    public Cart() {
    }

    public Cart(String id, String user_id, String created_at) {
        this.id = id;
        this.user_id = user_id;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
