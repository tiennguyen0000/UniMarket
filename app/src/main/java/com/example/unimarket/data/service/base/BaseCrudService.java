package com.example.unimarket.data.service.base;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BaseCrudService<T> implements Identifiable<T> {
    private final Map<String, T> store = new LinkedHashMap<>();

    protected abstract String getTableName();

    protected abstract Class<T> getModelClass();

    public synchronized List<T> getAll() {
        return new ArrayList<>(store.values());
    }

    public synchronized T getById(String id) {
        if (id == null) {
            return null;
        }
        return store.get(id);
    }

    public synchronized boolean create(T item) {
        if (item == null) {
            return false;
        }

        String id = getId(item);
        if (id == null || id.isEmpty()) {
            // Generate UUID if not provided
            id = UUID.randomUUID().toString();
            setId(item, id);
        }

        store.put(id, item);
        return true;
    }

    public synchronized boolean update(T item) {
        if (item == null) {
            return false;
        }

        String id = getId(item);
        if (id == null || !store.containsKey(id)) {
            return false;
        }

        store.put(id, item);
        return true;
    }

    public synchronized boolean delete(String id) {
        if (id == null) {
            return false;
        }
        return store.remove(id) != null;
    }
}
