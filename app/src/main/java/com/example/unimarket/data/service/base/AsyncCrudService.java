package com.example.unimarket.data.service.base;

import android.util.Log;
import com.example.unimarket.network.ApiResponse;
import com.example.unimarket.network.HttpApiClient;
import com.example.unimarket.network.NetworkExecutor;
import com.example.unimarket.network.SupabaseRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Async CRUD Service - Callback-based, không block main thread
 * TỐT cho CrudTestActivity và tất cả UI code
 */
public class AsyncCrudService {
    private static final String TAG = "AsyncCrudService";
    private static final HttpApiClient client = SupabaseRepository.getInstance().getClient();

    public interface ListCallback<T> {
        void onSuccess(List<T> data);
        void onError(String error);
    }

    public interface ItemCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public interface BooleanCallback {
        void onSuccess(boolean success);
        void onError(String error);
    }

    public static <T> void getAll(String table, Class<T> cls, ListCallback<T> cb) {
        NetworkExecutor.execute(
                () -> {
                    ApiResponse<List<T>> r = client.getAll(table, cls);
                    if (!r.isSuccess()) {
                        throw new Exception(r.getMessage() != null ? r.getMessage() : "Unknown error");
                    }
                    return r.getData() != null ? r.getData() : new ArrayList<>();
                },
                new NetworkExecutor.NetworkCallback<List<T>>() {
                    @Override
                    public void onSuccess(List<T> data) {
                        if (cb != null) {
                            cb.onSuccess(data);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error in getAll", e);
                        if (cb != null) {
                            cb.onError(e.getMessage());
                        }
                    }
                }
        );
    }

    public static <T> void getById(String table, Long id, Class<T> cls, ItemCallback<T> cb) {
        NetworkExecutor.execute(
                () -> {
                    ApiResponse<T> r = client.getById(table, id, cls);
                    if (!r.isSuccess()) {
                        throw new Exception(r.getMessage() != null ? r.getMessage() : "Unknown error");
                    }
                    return r.getData();
                },
                new NetworkExecutor.NetworkCallback<T>() {
                    @Override
                    public void onSuccess(T data) {
                        if (cb != null) {
                            cb.onSuccess(data);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error in getById", e);
                        if (cb != null) {
                            cb.onError(e.getMessage());
                        }
                    }
                }
        );
    }

    public static <T> void create(String table, T item, Class<T> cls, ItemCallback<T> cb) {
        NetworkExecutor.execute(
                () -> {
                    ApiResponse<T> r = client.create(table, item, cls);
                    if (!r.isSuccess()) {
                        throw new Exception(r.getMessage() != null ? r.getMessage() : "Unknown error");
                    }
                    return r.getData();
                },
                new NetworkExecutor.NetworkCallback<T>() {
                    @Override
                    public void onSuccess(T data) {
                        if (cb != null) {
                            cb.onSuccess(data);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error in create", e);
                        if (cb != null) {
                            cb.onError(e.getMessage());
                        }
                    }
                }
        );
    }

    public static <T> void update(String table, T item, Class<T> cls, ItemCallback<T> cb) {
        NetworkExecutor.execute(
                () -> {
                    ApiResponse<T> r = client.update(table, item, cls);
                    if (!r.isSuccess()) {
                        throw new Exception(r.getMessage() != null ? r.getMessage() : "Unknown error");
                    }
                    return r.getData();
                },
                new NetworkExecutor.NetworkCallback<T>() {
                    @Override
                    public void onSuccess(T data) {
                        if (cb != null) {
                            cb.onSuccess(data);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error in update", e);
                        if (cb != null) {
                            cb.onError(e.getMessage());
                        }
                    }
                }
        );
    }

    public static void delete(String table, Long id, BooleanCallback cb) {
        NetworkExecutor.execute(
                () -> {
                    ApiResponse<Void> r = client.delete(table, id);
                    if (!r.isSuccess()) {
                        throw new Exception(r.getMessage() != null ? r.getMessage() : "Unknown error");
                    }
                    return true;
                },
                new NetworkExecutor.NetworkCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        if (cb != null) {
                            cb.onSuccess(true);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error in delete", e);
                        if (cb != null) {
                            cb.onError(e.getMessage());
                        }
                    }
                }
        );
    }
}

