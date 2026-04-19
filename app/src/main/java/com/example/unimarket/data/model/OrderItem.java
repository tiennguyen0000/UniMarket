package com.example.unimarket.data.model;

public class OrderItem {
    private String id;
    private String order_id;
    private String product_id;
    private String seller_id;
    private Double price;

    public OrderItem() {
    }

    public OrderItem(String id, String order_id, String product_id, String seller_id, Double price) {
        this.id = id;
        this.order_id = order_id;
        this.product_id = product_id;
        this.seller_id = seller_id;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrder_id() { return order_id; }
    public void setOrder_id(String order_id) { this.order_id = order_id; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
