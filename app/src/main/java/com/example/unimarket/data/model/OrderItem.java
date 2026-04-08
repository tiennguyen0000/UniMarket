package com.example.unimarket.data.model;

public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private Long sellerId;
    private Double price;

    public OrderItem() {
    }

    public OrderItem(Long id, Long orderId, Long productId, Long sellerId, Double price) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.sellerId = sellerId;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
