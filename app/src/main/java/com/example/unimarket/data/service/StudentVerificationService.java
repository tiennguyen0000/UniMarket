package com.example.unimarket.data.service;

import com.example.unimarket.data.model.StudentVerification;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentVerificationService extends BaseCrudService<StudentVerification> {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void submitRequest(StudentVerification request, AsyncCrudService.ItemCallback<StudentVerification> callback) {
        if (request == null || request.getUser_id() == null || request.getUser_id().isEmpty()) {
            if (callback != null) callback.onError("Thiếu mã yêu cầu xác thực.");
            return;
        }

        db.collection(getTableName())
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    request.setId(documentReference.getId());
                    if (callback != null) callback.onSuccess(request);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(errorMessage(e));
                });
    }

    public void getPendingRequests(AsyncCrudService.ListCallback<StudentVerification> callback) {
        db.collection(getTableName())
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<StudentVerification> requests = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        StudentVerification item = doc.toObject(StudentVerification.class);
                        item.setId(doc.getId());
                        requests.add(item);
                    }
                    if (callback != null) callback.onSuccess(requests);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(errorMessage(e));
                });
    }

    public void approveRequest(String requestId, String reviewedAt, AsyncCrudService.BooleanCallback callback) {
        db.collection(getTableName())
                .document(requestId)
                .update("status", "approved", "reviewed_at", reviewedAt)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(errorMessage(e));
                });
    }

    private String errorMessage(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    @Override
    public String getId(StudentVerification item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(StudentVerification item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "student_verifications";
    }

    @Override
    protected Class<StudentVerification> getModelClass() {
        return StudentVerification.class;
    }
}
