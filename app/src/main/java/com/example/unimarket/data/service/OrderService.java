package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.service.base.BaseCrudService;

public class OrderService extends BaseCrudService<Order> {
    @Override
    public Long getId(Order item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Order item, Long id) {
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
