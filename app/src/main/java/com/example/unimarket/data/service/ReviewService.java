package com.example.unimarket.data.service;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;
import com.example.unimarket.data.service.base.Result;
import com.example.unimarket.data.service.base.ResultCallback;

import java.util.List;

public class ReviewService extends BaseCrudService<Review> {
    private final OrderService orderService = new OrderService();

    public void getReviewsByProductId(String productId,
                                      com.example.unimarket.data.service.base.AsyncCrudService.ListCallback<Review> callback) {
        getWithFilter("product_id", productId, callback);
    }

    public void getReviewsBySellerId(String sellerId,
                                     com.example.unimarket.data.service.base.AsyncCrudService.ListCallback<Review> callback) {
        getWithFilter("seller_id", sellerId, callback);
    }

    public void canReviewProduct(String buyerId, String productId, ResultCallback<Boolean> callback) {
        orderService.getOrdersByBuyerId(buyerId, new AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> data) {
                boolean allowed = false;
                if (data != null) {
                    for (Order order : data) {
                        if (order != null
                                && productId != null
                                && productId.equals(order.getProduct_id())
                                && DomainConstants.OrderStatus.DONE.equalsIgnoreCase(order.getStatus())) {
                            allowed = true;
                            break;
                        }
                    }
                }
                if (callback != null) callback.onResult(Result.success(allowed));
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onResult(Result.error(error));
            }
        });
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
        return DomainConstants.Collections.REVIEWS;
    }

    @Override
    protected Class<Review> getModelClass() {
        return Review.class;
    }
}
