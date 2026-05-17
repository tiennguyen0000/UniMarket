package com.example.unimarket.data.service;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ProductService extends BaseCrudService<Product> {

    public void getProductsBySellerId(String sellerId, AsyncCrudService.ListCallback<Product> callback) {
        getWithFilter("seller_id", sellerId, callback);
    }

    public void getActiveProducts(AsyncCrudService.ListCallback<Product> callback) {
        getWithFilter("status", DomainConstants.ProductStatus.ACTIVE, callback);
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
        return DomainConstants.Collections.PRODUCTS;
    }

    @Override
    protected Class<Product> getModelClass() {
        return Product.class;
    }
}
