package com.example.unimarket.data.service;

import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.base.BaseCrudService;

public class UserService extends BaseCrudService<User> {
    @Override
    public String getId(User item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(User item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "users";
    }

    @Override
    protected Class<User> getModelClass() {
        return User.class;
    }
}

