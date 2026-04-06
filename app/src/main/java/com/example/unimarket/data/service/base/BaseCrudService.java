package com.example.unimarket.data.service.base;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseCrudService<T> implements Identifiable<T> {
    private final Map<Long, T> store = new LinkedHashMap<>();
    private long nextId = 1L;

    public synchronized List<T> getAll() {
        return new ArrayList<>(store.values());
    }

    public synchronized T getById(Long id) {
        if (id == null) {
            return null;
        }
        return store.get(id);
    }

    public synchronized boolean create(T item) {
        if (item == null) {
            return false;
        }

        Long id = getId(item);
        if (id == null) {
            id = nextId++;
            setId(item, id);
        } else if (id >= nextId) {
            nextId = id + 1;
        }

        store.put(id, item);
        return true;
    }

    public synchronized boolean update(T item) {
        if (item == null) {
            return false;
        }

        Long id = getId(item);
        if (id == null || !store.containsKey(id)) {
            return false;
        }

        store.put(id, item);
        return true;
    }

    public synchronized boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        return store.remove(id) != null;
    }
}
