package com.example.unimarket.data.model;

public class ProductImage {
    private String id;
    private String product_id;
    private String image_url;

    public ProductImage() {
    }

    public ProductImage(String id, String product_id, String image_url) {
        this.id = id;
        this.product_id = product_id;
        this.image_url = image_url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }
}
