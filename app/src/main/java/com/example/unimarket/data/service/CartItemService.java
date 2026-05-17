package com.example.unimarket.data.service;

import com.example.unimarket.data.model.CartItem;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;

public class CartItemService extends BaseCrudService<CartItem> {

    public void getCartItemsByCartId(String cartId, AsyncCrudService.ListCallback<CartItem> callback) {
        getWithFilter("cart_id", cartId, callback);
    }

    @Override
    public String getId(CartItem item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(CartItem item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "cart_items";
    }

    @Override
    protected Class<CartItem> getModelClass() {
        return CartItem.class;
    }
}
