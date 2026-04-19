package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.service.base.BaseCrudService;

public class CategoryService extends BaseCrudService<Category> {
    @Override
    public String getId(Category item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Category item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "categories";
    }

    @Override
    protected Class<Category> getModelClass() {
        return Category.class;
    }
}
