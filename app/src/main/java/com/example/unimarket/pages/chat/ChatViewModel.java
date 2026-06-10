package com.example.unimarket.pages.chat;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Conversation;
import com.example.unimarket.data.model.Message;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ChatViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;
    private Conversation conversation;
    private String conversationId;

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public static String buildProductConversationId(String productId, String buyerId, String sellerId) {
        return "product_" + safeDocumentPart(productId)
                + "_" + safeDocumentPart(buyerId)
                + "_" + safeDocumentPart(sellerId);
    }

    public void startListening(Conversation conversation) {
        if (conversation == null || TextUtils.isEmpty(conversation.getId())) {
            isLoading.setValue(false);
            messages.setValue(new ArrayList<>());
            errorMessage.setValue("Không thể mở hội thoại.");
            return;
        }
        if (conversation.getId().equals(conversationId) && listenerRegistration != null) {
            return;
        }

        stopListening();
        this.conversation = conversation;
        this.conversationId = conversation.getId();
        isLoading.setValue(true);

        ensureConversation(conversation, this::listenForMessages);
    }

    public void sendMessage(String senderId, String content) {
        String trimmed = content != null ? content.trim() : "";
        if (TextUtils.isEmpty(conversationId) || trimmed.isEmpty()) return;
        if (TextUtils.isEmpty(senderId)
                || conversation == null
                || (!senderId.equals(conversation.getBuyer_id()) && !senderId.equals(conversation.getSeller_id()))) {
            errorMessage.setValue("Bạn không có quyền gửi tin nhắn trong cuộc hội thoại này.");
            return;
        }

        ensureConversation(conversation, () -> saveMessage(senderId, trimmed));
    }

    private void listenForMessages() {
        listenerRegistration = db.collection("messages")
                .whereEqualTo("conversation_id", conversationId)
                .addSnapshotListener((snapshots, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorMessage.setValue(readableError(error.getMessage(), "Không thể tải cuộc trò chuyện."));
                        return;
                    }

                    List<Message> result = new ArrayList<>();
                    if (snapshots != null) {
                        snapshots.forEach(doc -> {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                message.setId(doc.getId());
                                result.add(message);
                            }
                        });
                    }
                    result.sort(Comparator.comparing(this::safeCreatedAt));
                    messages.setValue(result);
                });
    }

    private void ensureConversation(Conversation data, Runnable onReady) {
        if (data == null || TextUtils.isEmpty(data.getId())) {
            isLoading.setValue(false);
            errorMessage.setValue("Không thể mở hội thoại.");
            return;
        }

        String now = nowIsoUtc();
        if (TextUtils.isEmpty(data.getCreated_at())) {
            data.setCreated_at(now);
        }
        data.setUpdated_at(now);

        db.collection("conversations")
                .document(data.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (onReady != null) onReady.run();
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(readableError(
                            e != null ? e.getMessage() : null,
                            "Không thể tạo cuộc hội thoại."
                    ));
                });
    }

    private void saveMessage(String senderId, String content) {
        String now = nowIsoUtc();
        Message message = new Message(null, conversationId, senderId, content, now);

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> updateConversationPreview(content, senderId, now))
                .addOnFailureListener(e -> errorMessage.setValue(readableError(
                        e != null ? e.getMessage() : null,
                        "Không thể gửi tin nhắn."
                )));
    }

    private void updateConversationPreview(String lastMessage, String senderId, String timestamp) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("updated_at", timestamp);
        updates.put("last_message", lastMessage);
        updates.put("last_sender_id", senderId);
        updates.put("last_message_at", timestamp);
        db.collection("conversations")
                .document(conversationId)
                .set(updates, SetOptions.merge())
                .addOnFailureListener(e -> errorMessage.setValue(readableError(
                        e != null ? e.getMessage() : null,
                        "Không thể cập nhật hội thoại."
                )));
    }

    private void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private String safeCreatedAt(Message message) {
        return message != null && message.getCreated_at() != null ? message.getCreated_at() : "";
    }

    private String readableError(String rawError, String fallback) {
        if (TextUtils.isEmpty(rawError)) return fallback;
        if (rawError.contains("PERMISSION_DENIED") || rawError.contains("Missing or insufficient permissions")) {
            return fallback;
        }
        return rawError;
    }

    private static String safeDocumentPart(String value) {
        if (TextUtils.isEmpty(value)) return "unknown";
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    @Override
    protected void onCleared() {
        stopListening();
        super.onCleared();
    }
}
