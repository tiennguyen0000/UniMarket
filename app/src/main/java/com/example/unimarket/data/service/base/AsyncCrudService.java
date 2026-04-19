package com.example.unimarket.data.service.base;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Async CRUD Service - Sử dụng Firebase Firestore
 */
public class AsyncCrudService {
    private static final String TAG = "AsyncCrudService";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    private static <T> String getItemId(T item) {
        if (item == null) {
            return null;
        }
        try {
            Object id = item.getClass().getMethod("getId").invoke(item);
            return id instanceof String ? (String) id : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static <T> void setItemId(T item, String id) {
        if (item == null || id == null || id.isEmpty()) {
            return;
        }
        try {
            item.getClass().getMethod("setId", String.class).invoke(item, id);
        } catch (Exception ignored) {
            // Models without setId are still supported.
        }
    }

    private static String buildErrorMessage(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    public static <T> void getWithFilter(String table, String column, String value, Class<T> cls, ListCallback<T> cb) {
        db.collection(table)
                .whereEqualTo(column, value)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<T> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        T item = doc.toObject(cls);
                        setItemId(item, doc.getId());
                        list.add(item);
                    }
                    if (cb != null) cb.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in getWithFilter: " + table, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
    }

    public static <T> void getAll(String table, Class<T> cls, ListCallback<T> cb) {
        db.collection(table)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<T> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        T item = doc.toObject(cls);
                        setItemId(item, doc.getId());
                        list.add(item);
                    }
                    if (cb != null) cb.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in getAll: " + table, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
    }

    public static <T> void getById(String table, String id, Class<T> cls, ItemCallback<T> cb) {
        if (id == null || id.isEmpty()) {
            if (cb != null) cb.onSuccess(null);
            return;
        }
        db.collection(table).document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        T item = documentSnapshot.toObject(cls);
                        setItemId(item, documentSnapshot.getId());
                        if (cb != null) cb.onSuccess(item);
                    } else {
                        if (cb != null) cb.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in getById: " + table + "/" + id, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
    }

    public static <T> void upsert(String table, T item, Class<T> cls, ItemCallback<T> cb) {
        String id = getItemId(item);

        if (id == null || id.isEmpty()) {
            db.collection(table).add(item)
                .addOnSuccessListener(documentReference -> {
                    setItemId(item, documentReference.getId());
                    if (cb != null) cb.onSuccess(item);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in upsert (add): " + table, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
        } else {
            db.collection(table).document(id).set(item, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (cb != null) cb.onSuccess(item);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in upsert (set): " + table + "/" + id, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
        }
    }

    public static <T> void create(String table, T item, Class<T> cls, ItemCallback<T> cb) {
        String id = getItemId(item);

        if (id != null && !id.isEmpty()) {
            db.collection(table).document(id).set(item)
                .addOnSuccessListener(v -> {
                    if (cb != null) cb.onSuccess(item);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in create (set): " + table + "/" + id, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
        } else {
            db.collection(table).add(item)
                .addOnSuccessListener(documentReference -> {
                    setItemId(item, documentReference.getId());
                    if (cb != null) cb.onSuccess(item);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in create (add): " + table, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
        }
    }

    public static <T> void update(String table, T item, Class<T> cls, ItemCallback<T> cb) {
        String id = getItemId(item);

        if (id == null || id.isEmpty()) {
            if (cb != null) cb.onError("Update failed: ID is missing");
            return;
        }

        db.collection(table).document(id).set(item, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (cb != null) cb.onSuccess(item);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in update: " + table + "/" + id, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
    }

    public static void delete(String table, String id, BooleanCallback cb) {
        if (id == null || id.isEmpty()) {
            if (cb != null) cb.onSuccess(false);
            return;
        }
        db.collection(table).document(id).delete()
                .addOnSuccessListener(v -> {
                    if (cb != null) cb.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in delete: " + table + "/" + id, e);
                    if (cb != null) cb.onError(buildErrorMessage(e));
                });
    }
}


