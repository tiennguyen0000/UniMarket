package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;

public class NotificationService extends BaseCrudService<Notification> {
    public void getNotificationsByUserId(String userId, AsyncCrudService.ListCallback<Notification> callback) {
        getWithFilter("user_id", userId, callback);
    }

    @Override
    public String getId(Notification item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Notification item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "notifications";
    }

    @Override
    protected Class<Notification> getModelClass() {
        return Notification.class;
    }
}
