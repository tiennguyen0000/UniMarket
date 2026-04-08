package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Message;
import com.example.unimarket.data.service.base.BaseCrudService;

public class MessageService extends BaseCrudService<Message> {
    @Override
    public Long getId(Message item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Message item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "messages";
    }

    @Override
    protected Class<Message> getModelClass() {
        return Message.class;
    }
}
