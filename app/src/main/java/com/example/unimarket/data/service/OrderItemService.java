package com.example.unimarket.data.service;

import com.example.unimarket.data.model.OrderItem;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;

public class OrderItemService extends BaseCrudService<OrderItem> {

    public void getOrderItemsByOrderId(String orderId, AsyncCrudService.ListCallback<OrderItem> callback) {
        getWithFilter("order_id", orderId, callback);
    }

    @Override
    public String getId(OrderItem item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(OrderItem item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "order_items";
    }

    @Override
    protected Class<OrderItem> getModelClass() {
        return OrderItem.class;
    }
}
