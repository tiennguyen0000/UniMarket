package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ReviewService extends BaseCrudService<Review> {

    public void getReviewsByProductId(String productId,
                                      com.example.unimarket.data.service.base.AsyncCrudService.ListCallback<Review> callback) {
        getWithFilter("product_id", productId, callback);
    }

    public void getReviewsBySellerId(String sellerId,
                                     com.example.unimarket.data.service.base.AsyncCrudService.ListCallback<Review> callback) {
        getWithFilter("seller_id", sellerId, callback);
    }
    @Override
    public String getId(Review item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Review item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "reviews";
    }

    @Override
    protected Class<Review> getModelClass() {
        return Review.class;
    }
}
