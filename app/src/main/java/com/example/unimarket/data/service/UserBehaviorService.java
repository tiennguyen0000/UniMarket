package com.example.unimarket.data.service;

import com.example.unimarket.data.model.UserBehavior;
import com.example.unimarket.data.service.base.BaseCrudService;

public class UserBehaviorService extends BaseCrudService<UserBehavior> {
    @Override
    public Long getId(UserBehavior item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(UserBehavior item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "user_behavior";
    }

    @Override
    protected Class<UserBehavior> getModelClass() {
        return UserBehavior.class;
    }
}
