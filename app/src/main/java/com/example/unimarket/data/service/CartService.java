package com.example.unimarket.data.service;
import com.example.unimarket.data.model.Cart;
import com.example.unimarket.data.service.base.BaseCrudService;

public class CartService extends BaseCrudService<Cart> {
    @Override
    public Long getId(Cart item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Cart item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "carts";
    }

    @Override
    protected Class<Cart> getModelClass() {
        return Cart.class;
    }
}
