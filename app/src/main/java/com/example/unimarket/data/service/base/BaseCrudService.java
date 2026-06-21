package com.example.unimarket.data.service.base;

import java.util.List;

public abstract class BaseCrudService<T> implements Identifiable<T> {
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
}
