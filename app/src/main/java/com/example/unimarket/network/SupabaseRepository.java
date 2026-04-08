package com.example.unimarket.network;

import android.util.Log;

import com.example.unimarket.utils.Constants;

/**
 * Repository để quản lý kết nối Supabase
 */
public class SupabaseRepository {
    private static SupabaseRepository instance;
    private final HttpApiClient client;
    private static final String TAG = "SupabaseRepository";

    private SupabaseRepository() {
        this.client = new HttpApiClient(
                Constants.SUPABASE_URL,
                Constants.SUPABASE_ANON_KEY
        );
    }

    public static synchronized SupabaseRepository getInstance() {
        if (instance == null) {
            instance = new SupabaseRepository();
        }
        return instance;
    }

    public HttpApiClient getClient() {
        return client;
    }

    /**
     * Test kết nối Supabase (async)
     */
    public void testConnection(ConnectionCallback callback) {
        NetworkExecutor.execute(
                () -> {
                    try {
                        ApiResponse<?> response = client.getAll("users", Object.class);
                        return response.isSuccess();
                    } catch (Exception e) {
                        Log.e(TAG, "Test connection failed", e);
                        return false;
                    }
                },
                new NetworkExecutor.NetworkCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (callback != null) {
                            callback.onConnectionTest(result);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Connection test error", e);
                        if (callback != null) {
                            callback.onConnectionTest(false);
                        }
                    }
                }
        );
    }

    public interface ConnectionCallback {
        void onConnectionTest(boolean success);
    }
}

