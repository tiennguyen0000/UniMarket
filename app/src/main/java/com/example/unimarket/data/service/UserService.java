package com.example.unimarket.data.service;

import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserService extends BaseCrudService<User> {

    public void upsertProfile(User user, AsyncCrudService.ItemCallback<User> callback) {
        if (user == null || user.getId() == null || user.getId().isEmpty()) {
            if (callback != null) callback.onError("Profile id is missing");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(getTableName())
                .document(user.getId())
                .set(toFirestoreMap(user), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        String message = e != null && e.getMessage() != null
                                ? e.getMessage()
                                : "Unknown error";
                        callback.onError(message);
                    }
                });
    }

    public void getProfileById(String userId, AsyncCrudService.ItemCallback<User> callback) {
        getById(userId, callback);
    }

    public void setStudentVerified(String userId, boolean verified, AsyncCrudService.BooleanCallback callback) {
        FirebaseFirestore.getInstance()
                .collection(getTableName())
                .document(userId)
                .update("_verified", verified)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e != null && e.getMessage() != null ? e.getMessage() : "Unknown error");
                    }
                });
    }

    public void setAccountStatus(String userId, String status, String updatedAt,
                                 AsyncCrudService.BooleanCallback callback) {
        FirebaseFirestore.getInstance()
                .collection(getTableName())
                .document(userId)
                .update("account_status", status, "updated_at", updatedAt)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e != null && e.getMessage() != null ? e.getMessage() : "Unknown error");
                    }
                });
    }

    @Override
    public String getId(User item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(User item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "profiles";
    }

    @Override
    protected Class<User> getModelClass() {
        return User.class;
    }

    private Map<String, Object> toFirestoreMap(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("full_name", user.getFull_name());
        data.put("phone", user.getPhone());
        data.put("university", user.getUniversity());
        data.put("avatar_url", user.getAvatar_url());
        data.put("role", user.getRole() != null ? user.getRole() : "user");
        data.put("account_status", user.getAccount_status() != null ? user.getAccount_status() : "active");
        data.put("_verified", user.isVerified());
        data.put("created_at", user.getCreated_at());
        data.put("updated_at", user.getUpdated_at());
        return data;
    }
}
