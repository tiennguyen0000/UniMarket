package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.service.base.BaseCrudService;

import com.example.unimarket.data.service.base.AsyncCrudService;
import java.util.List;

public class OrderService extends BaseCrudService<Order> {
    
    public void getOrdersByBuyerId(String buyerId, AsyncCrudService.ListCallback<Order> callback) {
        AsyncCrudService.getWithFilter(getTableName(), "buyer_id", buyerId, getModelClass(), callback);
    }

    @Override
    public String getId(Order item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Order item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "orders";
    }

    @Override
    protected Class<Order> getModelClass() {
        return Order.class;
    }
}
