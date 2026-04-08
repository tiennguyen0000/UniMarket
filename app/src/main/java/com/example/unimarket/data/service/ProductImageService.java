package com.example.unimarket.data.service;

import com.example.unimarket.data.model.ProductImage;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ProductImageService extends BaseCrudService<ProductImage> {
    @Override
    public Long getId(ProductImage item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(ProductImage item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "product_images";
    }

    @Override
    protected Class<ProductImage> getModelClass() {
        return ProductImage.class;
    }
}
