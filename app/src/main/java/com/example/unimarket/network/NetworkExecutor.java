package com.example.unimarket.network;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper để thực hiện network requests trên background thread
 * và post results về main thread
 */
public class NetworkExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Chạy task trên background thread
     */
    public static <T> void execute(NetworkTask<T> task, NetworkCallback<T> callback) {
        executor.execute(() -> {
            try {
                T result = task.execute();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }

    /**
     * Interface cho task cần thực hiện
     */
    public interface NetworkTask<T> {
        T execute() throws Exception;
    }

    /**
     * Interface cho callback results
     */
    public interface NetworkCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Shutdown executor (gọi khi app đóng)
     */
    public static void shutdown() {
        executor.shutdown();
    }
}

