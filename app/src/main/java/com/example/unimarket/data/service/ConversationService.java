package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Conversation;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ConversationService extends BaseCrudService<Conversation> {
    @Override
    public Long getId(Conversation item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Conversation item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "conversations";
    }

    @Override
    protected Class<Conversation> getModelClass() {
        return Conversation.class;
    }
}
