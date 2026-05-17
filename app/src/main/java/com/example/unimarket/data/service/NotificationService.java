package com.example.unimarket.data.service;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;
import com.example.unimarket.data.service.base.ResultCallback;
import com.example.unimarket.data.util.FirestoreIds;
import com.example.unimarket.data.util.TimeUtils;

public class NotificationService extends BaseCrudService<Notification> {
    public void getNotificationsByUserId(String userId, AsyncCrudService.ListCallback<Notification> callback) {
        getWithFilter("user_id", userId, callback);
    }

    public void createNotification(String userId, String title, String content, String type,
                                   String targetId, ResultCallback<Notification> callback) {
        Notification notification = new Notification();
        notification.setId(FirestoreIds.stableDocId(
                "notification",
                userId,
                targetId,
                String.valueOf(System.currentTimeMillis())));
        notification.setUser_id(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setTarget_id(targetId);
        notification.setIs_read(false);
        notification.setCreated_at(TimeUtils.nowIsoUtc());
        save(notification, callback);
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
        return DomainConstants.Collections.NOTIFICATIONS;
    }

    @Override
    protected Class<Notification> getModelClass() {
        return Notification.class;
    }
}
