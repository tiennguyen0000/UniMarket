package com.example.unimarket.data.service.base;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseCrudService<T> implements Identifiable<T> {
    private final Map<String, T> memoryStore = new LinkedHashMap<>();

    protected abstract String getTableName();

    protected abstract Class<T> getModelClass();

    public void getAll(AsyncCrudService.ListCallback<T> callback) {
        AsyncCrudService.getAll(getTableName(), getModelClass(), callback);
    }

    public void getById(String id, AsyncCrudService.ItemCallback<T> callback) {
        AsyncCrudService.getById(getTableName(), id, getModelClass(), callback);
    }

    public void getWithFilter(String column, String value, AsyncCrudService.ListCallback<T> callback) {
        AsyncCrudService.getWithFilter(getTableName(), column, value, getModelClass(), callback);
    }

    public void upsert(T item, AsyncCrudService.ItemCallback<T> callback) {
        AsyncCrudService.upsert(getTableName(), item, getModelClass(), callback);
    }

    public void create(T item, AsyncCrudService.ItemCallback<T> callback) {
        AsyncCrudService.create(getTableName(), item, getModelClass(), callback);
    }

    public void update(T item, AsyncCrudService.ItemCallback<T> callback) {
        AsyncCrudService.update(getTableName(), item, getModelClass(), callback);
    }

    public void deleteById(String id, AsyncCrudService.BooleanCallback callback) {
        AsyncCrudService.delete(getTableName(), id, callback);
    }

    public void fetchAll(ResultCallback<List<T>> callback) {
        getAll(new AsyncCrudService.ListCallback<T>() {
            @Override
            public void onSuccess(List<T> data) {
                if (callback != null) {
                    callback.onResult(Result.success(data));
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onResult(Result.error(error));
                }
            }
        });
    }

    public void fetchById(String id, ResultCallback<T> callback) {
        getById(id, new AsyncCrudService.ItemCallback<T>() {
            @Override
            public void onSuccess(T data) {
                if (callback != null) {
                    callback.onResult(Result.success(data));
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onResult(Result.error(error));
                }
            }
        });
    }

    public void save(T item, ResultCallback<T> callback) {
        upsert(item, new AsyncCrudService.ItemCallback<T>() {
            @Override
            public void onSuccess(T data) {
                if (callback != null) {
                    callback.onResult(Result.success(data));
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onResult(Result.error(error));
                }
            }
        });
    }

    /**
     * Legacy synchronous methods kept for existing unit tests.
     * Production code should use async/result-based APIs.
     */
    @Deprecated
    public synchronized List<T> getAll() {
        return new ArrayList<>(memoryStore.values());
    }

    @Deprecated
    public synchronized T getById(String id) {
        if (id == null) {
            return null;
        }
        return memoryStore.get(id);
    }

    @Deprecated
    public synchronized boolean create(T item) {
        if (item == null) {
            return false;
        }
        String id = getId(item);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            setId(item, id);
        }
        memoryStore.put(id, item);
        return true;
    }

    @Deprecated
    public synchronized boolean update(T item) {
        if (item == null) {
            return false;
        }
        String id = getId(item);
        if (id == null || !memoryStore.containsKey(id)) {
            return false;
        }
        memoryStore.put(id, item);
        return true;
    }

    @Deprecated
    public synchronized boolean delete(String id) {
        if (id == null) {
            return false;
        }
        return memoryStore.remove(id) != null;
    }
}
