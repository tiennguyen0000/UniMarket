package com.example.unimarket.data.model;

public class Order {
    private String id;
    private String buyer_id;
    private String seller_id;
    private String product_id;
    private String product_title;
    private String product_image_url;
    private Integer quantity;
    private Double unit_price;
    private Double subtotal_price;
    private Double shipping_fee;
    private Double seller_amount;
    private String discount_code;
    private Double discount_amount;
    private Double total_price;
    private String buyer_phone;
    private String delivery_location;
    private String shipping_method;
    private String buyer_note;
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
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getUnit_price() { return unit_price; }
    public void setUnit_price(Double unit_price) { this.unit_price = unit_price; }
    public Double getSubtotal_price() { return subtotal_price; }
    public void setSubtotal_price(Double subtotal_price) { this.subtotal_price = subtotal_price; }
    public Double getShipping_fee() { return shipping_fee; }
    public void setShipping_fee(Double shipping_fee) { this.shipping_fee = shipping_fee; }
    public Double getSeller_amount() { return seller_amount; }
    public void setSeller_amount(Double seller_amount) { this.seller_amount = seller_amount; }
    public String getDiscount_code() { return discount_code; }
    public void setDiscount_code(String discount_code) { this.discount_code = discount_code; }
    public Double getDiscount_amount() { return discount_amount; }
    public void setDiscount_amount(Double discount_amount) { this.discount_amount = discount_amount; }
    public Double getTotal_price() { return total_price; }
    public void setTotal_price(Double total_price) { this.total_price = total_price; }
    public String getBuyer_phone() { return buyer_phone; }
    public void setBuyer_phone(String buyer_phone) { this.buyer_phone = buyer_phone; }
    public String getDelivery_location() { return delivery_location; }
    public void setDelivery_location(String delivery_location) { this.delivery_location = delivery_location; }
    public String getShipping_method() { return shipping_method; }
    public void setShipping_method(String shipping_method) { this.shipping_method = shipping_method; }
    public String getBuyer_note() { return buyer_note; }
    public void setBuyer_note(String buyer_note) { this.buyer_note = buyer_note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
