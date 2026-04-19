package com.example.unimarket.data.model;

public class  Product {
    private String id;
    private String seller_id;
    private String title;
    private String description;
    private Double price;
    private String category_id;
    private String condition;
    private String status;
    private String created_at;
    private String updated_at;

    public Product() {
    }

    public Product(String id, String seller_id, String title, String description, Double price,
                   String category_id, String condition, String status, String created_at, String updated_at) {
        this.id = id;
        this.seller_id = seller_id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category_id = category_id;
        this.condition = condition;
        this.status = status;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getCategory_id() { return category_id; }
    public void setCategory_id(String category_id) { this.category_id = category_id; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
