package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ProductService extends BaseCrudService<Product> {
    @Override
    public Long getId(Product item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Product item, Long id) {
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
