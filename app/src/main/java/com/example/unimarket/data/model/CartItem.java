package com.example.unimarket.data.model;

public class CartItem {
    private String id;
    private String cart_id;
    private String product_id;
    private Integer quantity;

    public CartItem() {
    }

    public CartItem(String id, String cart_id, String product_id, Integer quantity) {
        this.id = id;
        this.cart_id = cart_id;
        this.product_id = product_id;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCart_id() { return cart_id; }
    public void setCart_id(String cart_id) { this.cart_id = cart_id; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
