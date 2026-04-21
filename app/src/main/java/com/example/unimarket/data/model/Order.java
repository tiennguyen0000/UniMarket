package com.example.unimarket.data.model;

public class Order {
    private String id;
    private String buyer_id;
    private String seller_id;
    private String product_id;
    private String product_title;
    private String product_image_url;
    private Double total_price;
    private String status;   // pending | confirmed | shipping | done | cancelled
    private String created_at;
    private String updated_at;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyer_id() { return buyer_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public String getProduct_title() { return product_title; }
    public void setProduct_title(String product_title) { this.product_title = product_title; }
    public String getProduct_image_url() { return product_image_url; }
    public void setProduct_image_url(String product_image_url) { this.product_image_url = product_image_url; }
    public Double getTotal_price() { return total_price; }
    public void setTotal_price(Double total_price) { this.total_price = total_price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
