package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.base.BaseCrudService;

import com.example.unimarket.data.service.base.AsyncCrudService;
import java.util.List;

public class ProductService extends BaseCrudService<Product> {

    public void getProductsBySellerId(String sellerId, AsyncCrudService.ListCallback<Product> callback) {
        AsyncCrudService.getWithFilter(getTableName(), "seller_id", sellerId, getModelClass(), callback);
    }

    @Override
    public String getId(Product item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Product item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "products";
    }

    @Override
    protected Class<Product> getModelClass() {
        return Product.class;
    }
}
