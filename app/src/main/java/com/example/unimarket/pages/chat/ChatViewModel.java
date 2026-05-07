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
            errorMessage.setValue("Không thể mở hội thoại.");
            return;
        }
        if (conversation.getId().equals(conversationId) && listenerRegistration != null) {
            return;
        }

        stopListening();
        this.conversation = conversation;
        this.conversationId = conversation.getId();
        ensureConversation(conversation);

        isLoading.setValue(true);
        listenerRegistration = db.collection("messages")
                .whereEqualTo("conversation_id", conversationId)
                .addSnapshotListener((snapshots, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorMessage.setValue(error.getMessage());
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

    public void sendMessage(String senderId, String content) {
        String trimmed = content != null ? content.trim() : "";
        if (TextUtils.isEmpty(conversationId) || trimmed.isEmpty()) return;

        String now = nowIsoUtc();
        String safeSenderId = !TextUtils.isEmpty(senderId) ? senderId : "guest_user";
        Message message = new Message(null, conversationId, safeSenderId, trimmed, now);

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> updateConversationPreview(trimmed, safeSenderId, now))
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    private void ensureConversation(Conversation data) {
        String now = nowIsoUtc();
        if (TextUtils.isEmpty(data.getCreated_at())) {
            data.setCreated_at(now);
        }
        data.setUpdated_at(now);
        db.collection("conversations")
                .document(data.getId())
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    private void updateConversationPreview(String lastMessage, String senderId, String timestamp) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("updated_at", timestamp);
        updates.put("last_message", lastMessage);
        updates.put("last_sender_id", senderId);
        updates.put("last_message_at", timestamp);
        db.collection("conversations")
                .document(conversationId)
                .set(updates, SetOptions.merge());
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
