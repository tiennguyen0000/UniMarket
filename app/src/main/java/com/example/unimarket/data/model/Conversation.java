package com.example.unimarket.data.model;

public class Conversation {
    private String id;
    private String created_at;
    private String updated_at;
    private String product_id;
    private String product_title;
    private String product_image_url;
    private String buyer_id;
    private String seller_id;
    private String buyer_name;
    private String seller_name;
    private String last_message;
    private String last_sender_id;
    private String last_message_at;

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
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public String getProduct_title() { return product_title; }
    public void setProduct_title(String product_title) { this.product_title = product_title; }
    public String getProduct_image_url() { return product_image_url; }
    public void setProduct_image_url(String product_image_url) { this.product_image_url = product_image_url; }
    public String getBuyer_id() { return buyer_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public String getBuyer_name() { return buyer_name; }
    public void setBuyer_name(String buyer_name) { this.buyer_name = buyer_name; }
    public String getSeller_name() { return seller_name; }
    public void setSeller_name(String seller_name) { this.seller_name = seller_name; }
    public String getLast_message() { return last_message; }
    public void setLast_message(String last_message) { this.last_message = last_message; }
    public String getLast_sender_id() { return last_sender_id; }
    public void setLast_sender_id(String last_sender_id) { this.last_sender_id = last_sender_id; }
    public String getLast_message_at() { return last_message_at; }
    public void setLast_message_at(String last_message_at) { this.last_message_at = last_message_at; }
}
