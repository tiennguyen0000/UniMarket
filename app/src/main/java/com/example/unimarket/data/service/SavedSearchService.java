package com.example.unimarket.data.service;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.SavedSearch;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;

public class SavedSearchService extends BaseCrudService<SavedSearch> {
    public void getSavedSearchesByUserId(String userId, AsyncCrudService.ListCallback<SavedSearch> callback) {
        getWithFilter("user_id", userId, callback);
    }

    @Override
    public String getId(SavedSearch item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(SavedSearch item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return DomainConstants.Collections.SAVED_SEARCHES;
    }

    @Override
    protected Class<SavedSearch> getModelClass() {
        return SavedSearch.class;
    }
}
