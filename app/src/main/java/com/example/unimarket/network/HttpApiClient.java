package com.example.unimarket.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Client để kết nối với Supabase REST API
 */
public class HttpApiClient {
    private static final String TAG = "HttpApiClient";
    private static final int TIMEOUT_SECONDS = 30;

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final Gson gson;

    public HttpApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * GET tất cả records từ một bảng
     */
    public <T> ApiResponse<List<T>> getAll(String tableName, Class<T> itemClass) {
        try {
            String url = getTableEndpoint(tableName);
            Request request = buildGetRequest(url);

            Response response = httpClient.newCall(request).execute();
            return handleListResponse(response, itemClass);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all from " + tableName, e);
            return new ApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * GET một record bằng ID
     */
    public <T> ApiResponse<T> getById(String tableName, String id, Class<T> itemClass) {
        try {
            String url = getTableEndpoint(tableName) + "?id=eq." + id;
            Request request = buildGetRequest(url);

            Response response = httpClient.newCall(request).execute();
            return handleSingleResponse(response, itemClass);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching " + tableName + " id=" + id, e);
            return new ApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * POST (Create) một record mới
     */
    public <T> ApiResponse<T> create(String tableName, T item, Class<T> itemClass) {
        try {
            String url = getTableEndpoint(tableName);
            String json = gson.toJson(item);
            Request request = buildPostRequest(url, json);

            Response response = httpClient.newCall(request).execute();
            return handleSingleResponse(response, itemClass);
        } catch (Exception e) {
            Log.e(TAG, "Error creating in " + tableName, e);
            return new ApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * PATCH (Update) một record
     */
    public <T> ApiResponse<T> update(String tableName, T item, Class<T> itemClass) {
        try {
            String url = getTableEndpoint(tableName);
            String json = gson.toJson(item);
            Request request = buildPatchRequest(url, json);

            Response response = httpClient.newCall(request).execute();
            return handleSingleResponse(response, itemClass);
        } catch (Exception e) {
            Log.e(TAG, "Error updating in " + tableName, e);
            return new ApiResponse<>(false, null, e.getMessage());
        }
    }

    /**
     * DELETE một record bằng ID
     */
    public ApiResponse<Void> delete(String tableName, String id) {
        try {
            String url = getTableEndpoint(tableName) + "?id=eq." + id;
            Request request = buildDeleteRequest(url);

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                return new ApiResponse<>(true, null, "Deleted successfully");
            } else {
                String body = response.body() != null ? response.body().string() : "";
                return new ApiResponse<>(false, null, "Delete failed: " + body);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting from " + tableName + " id=" + id, e);
            return new ApiResponse<>(false, null, e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private String getTableEndpoint(String tableName) {
        return baseUrl + "/rest/v1/" + tableName;
    }

    private Request buildGetRequest(String url) {
        return new Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .get()
                .build();
    }

    private Request buildPostRequest(String url, String json) {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        return new Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();
    }

    private Request buildPatchRequest(String url, String json) {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        return new Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .patch(body)
                .build();
    }

    private Request buildDeleteRequest(String url) {
        return new Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .delete()
                .build();
    }

    private <T> ApiResponse<T> handleSingleResponse(Response response, Class<T> itemClass) throws IOException {
        String body = response.body() != null ? response.body().string() : "";

        if (response.isSuccessful()) {
            if (body.isEmpty()) {
                return new ApiResponse<>(true, null, "Success");
            }

            // Supabase trả về array, lấy phần tử đầu tiên
            if (body.startsWith("[")) {
                List<T> list = gson.fromJson(body, new TypeToken<List<T>>(){}.getType());
                T item = !list.isEmpty() ? list.get(0) : null;
                return new ApiResponse<>(true, item, "Success");
            } else {
                T item = gson.fromJson(body, itemClass);
                return new ApiResponse<>(true, item, "Success");
            }
        } else {
            return new ApiResponse<>(false, null, "HTTP " + response.code() + ": " + body);
        }
    }

    private <T> ApiResponse<List<T>> handleListResponse(Response response, Class<T> itemClass) throws IOException {
        String body = response.body() != null ? response.body().string() : "";

        if (response.isSuccessful()) {
            if (body.isEmpty() || "[]".equals(body)) {
                return new ApiResponse<>(true, List.of(), "Success");
            }

            // Use a Type that includes the actual item class to prevent LinkedTreeMap deserialization
            Type listType = new TypeToken<List<T>>(){}.getType();
            List<T> items = gson.fromJson(body, listType);
            
            // If items came back as LinkedTreeMap instead of the proper type, convert them
            if (!items.isEmpty() && items.get(0) != null && !itemClass.isInstance(items.get(0))) {
                items = gson.fromJson(body, TypeToken.getParameterized(List.class, itemClass).getType());
            }
            
            return new ApiResponse<>(true, items, "Success");
        } else {
            return new ApiResponse<>(false, null, "HTTP " + response.code() + ": " + body);
        }
    }
}

