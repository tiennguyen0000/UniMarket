package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ReviewService extends BaseCrudService<Review> {
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
