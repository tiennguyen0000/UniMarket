package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Wishlist;
import com.example.unimarket.data.service.base.BaseCrudService;

public class WishlistService extends BaseCrudService<Wishlist> {
    @Override
    public String getId(Wishlist item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Wishlist item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "wishlist";
    }

    @Override
    protected Class<Wishlist> getModelClass() {
        return Wishlist.class;
    }
}
