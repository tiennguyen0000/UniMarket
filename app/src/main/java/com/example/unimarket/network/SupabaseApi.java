package com.example.unimarket.network;

import android.util.Log;

import com.example.unimarket.utils.Constants;
import com.google.gson.JsonElement;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Supabase API Client - Đơn giản, sử dụng OkHttp callback
 */
public class SupabaseApi {
    private static final String TAG = "SupabaseApi";
    private static final OkHttpClient client = new OkHttpClient();

    public interface ApiCallback {
        void onSuccess(String responseBody);
        void onError(String error);
    }

    /**
     * GET - Lấy dữ liệu từ bảng
     * Ví dụ: getAll("users", callback)
     */
    public static void getAll(String tableName, ApiCallback callback) {
        String url = Constants.SUPABASE_URL + "/rest/v1/" + tableName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GET " + tableName + " failed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "GET " + tableName + " code: " + response.code());

                if (callback != null) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(body);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + body);
                    }
                }
            }
        });
    }

    /**
     * GET by ID
     */
    public static void getById(String tableName, String id, ApiCallback callback) {
        String url = Constants.SUPABASE_URL + "/rest/v1/" + tableName + "?id=eq." + id;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GET " + tableName + " id=" + id + " failed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "GET " + tableName + " id=" + id + " code: " + response.code());

                if (callback != null) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(body);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + body);
                    }
                }
            }
        });
    }

    /**
     * POST - Tạo record mới
     */
    public static void create(String tableName, String jsonBody, ApiCallback callback) {
        String url = Constants.SUPABASE_URL + "/rest/v1/" + tableName;

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "POST " + tableName + " failed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "POST " + tableName + " code: " + response.code());

                if (callback != null) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + responseBody);
                    }
                }
            }
        });
    }

    /**
     * PATCH - Cập nhật record
     */
    public static void update(String tableName, String id, String jsonBody, ApiCallback callback) {
        String url = Constants.SUPABASE_URL + "/rest/v1/" + tableName + "?id=eq." + id;

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "PATCH " + tableName + " id=" + id + " failed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "PATCH " + tableName + " id=" + id + " code: " + response.code());

                if (callback != null) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + responseBody);
                    }
                }
            }
        });
    }

    /**
     * DELETE
     */
    public static void delete(String tableName, String id, ApiCallback callback) {
        String url = Constants.SUPABASE_URL + "/rest/v1/" + tableName + "?id=eq." + id;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "DELETE " + tableName + " id=" + id + " failed", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "DELETE " + tableName + " id=" + id + " code: " + response.code());

                if (callback != null) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + responseBody);
                    }
                }
            }
        });
    }
}

