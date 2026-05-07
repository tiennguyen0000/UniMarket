package com.example.unimarket.data.model;

public class UserBehavior {
    private String id;
    private String user_id;
    private String product_id;
    private String action;
    private String created_at;

    public UserBehavior() {
    }

    public UserBehavior(String id, String user_id, String product_id, String action, String created_at) {
        this.id = id;
        this.user_id = user_id;
        this.product_id = product_id;
        this.action = action;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
