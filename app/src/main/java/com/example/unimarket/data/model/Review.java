package com.example.unimarket.data.model;

public class Review {
    private String id;
    private String product_id;
    private String seller_id;
    private String reviewer_id;
    private String reviewer_name;
    private String reviewer_avatar;
    private Integer rating;
    private String title;
    private String content;
    private Long created_at_timestamp;
    private Integer helpful_count;

    public Review() {
    }

    public Review(String id, String product_id, String seller_id, String reviewer_id,
                  String reviewer_name, String reviewer_avatar, Integer rating,
                  String title, String content, Long created_at_timestamp, Integer helpful_count) {
        this.id = id;
        this.product_id = product_id;
        this.seller_id = seller_id;
        this.reviewer_id = reviewer_id;
        this.reviewer_name = reviewer_name;
        this.reviewer_avatar = reviewer_avatar;
        this.rating = rating;
        this.title = title;
        this.content = content;
        this.created_at_timestamp = created_at_timestamp;
        this.helpful_count = helpful_count;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public String getSeller_id() { return seller_id; }
    public void setSeller_id(String seller_id) { this.seller_id = seller_id; }

    public String getReviewer_id() { return reviewer_id; }
    public void setReviewer_id(String reviewer_id) { this.reviewer_id = reviewer_id; }

    public String getReviewer_name() { return reviewer_name; }
    public void setReviewer_name(String reviewer_name) { this.reviewer_name = reviewer_name; }

    public String getReviewer_avatar() { return reviewer_avatar; }
    public void setReviewer_avatar(String reviewer_avatar) { this.reviewer_avatar = reviewer_avatar; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getCreated_at_timestamp() { return created_at_timestamp; }
    public void setCreated_at_timestamp(Long created_at_timestamp) { this.created_at_timestamp = created_at_timestamp; }

    public Integer getHelpful_count() { return helpful_count; }
    public void setHelpful_count(Integer helpful_count) { this.helpful_count = helpful_count; }
}
