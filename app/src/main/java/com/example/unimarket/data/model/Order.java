package com.example.unimarket.data.model;

public class Order {
    private String id;
    private String buyer_id;
    private Double total_price;
    private String status;
    private String created_at;
    private String updated_at;

    public Order() {
    }

    public Order(String id, String buyer_id, Double total_price, String status, String created_at, String updated_at) {
        this.id = id;
        this.buyer_id = buyer_id;
        this.total_price = total_price;
        this.status = status;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyer_id() { return buyer_id; }
    public void setBuyer_id(String buyer_id) { this.buyer_id = buyer_id; }
    public Double getTotal_price() { return total_price; }
    public void setTotal_price(Double total_price) { this.total_price = total_price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
