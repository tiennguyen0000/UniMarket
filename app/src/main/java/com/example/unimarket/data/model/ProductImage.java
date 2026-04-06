package com.example.unimarket.data.model;

public class ProductImage {
    private Long id;
    private Long productId;
    private String imageUrl;

    public ProductImage() {
    }

    public ProductImage(Long id, Long productId, String imageUrl) {
        this.id = id;
        this.productId = productId;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
