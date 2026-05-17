package com.example.unimarket.pages.chat;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Conversation;
import com.example.unimarket.data.model.Message;
import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.util.TimeUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationService notificationService = new NotificationService();
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

        String now = TimeUtils.nowIsoUtc();
        String safeSenderId = !TextUtils.isEmpty(senderId) ? senderId : "guest_user";
        Message message = new Message(null, conversationId, safeSenderId, trimmed, now);

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> updateConversationPreview(trimmed, safeSenderId, now))
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    private void ensureConversation(Conversation data) {
        String now = TimeUtils.nowIsoUtc();
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
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(v -> notifyRecipient(lastMessage, senderId));
    }

    private void notifyRecipient(String lastMessage, String senderId) {
        if (conversation == null || TextUtils.isEmpty(senderId)) {
            return;
        }
        String recipientId = senderId.equals(conversation.getBuyer_id())
                ? conversation.getSeller_id()
                : conversation.getBuyer_id();
        if (TextUtils.isEmpty(recipientId) || recipientId.equals(senderId)) {
            return;
        }
        String productTitle = !TextUtils.isEmpty(conversation.getProduct_title())
                ? conversation.getProduct_title()
                : "sản phẩm";
        notificationService.createNotification(
                recipientId,
                "Tin nhắn mới",
                "Có phản hồi mới về " + productTitle + ": " + lastMessage,
                DomainConstants.NotificationType.CHAT,
                conversationId,
                result -> {
                });
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

    @Override
    protected void onCleared() {
        stopListening();
        super.onCleared();
    }
}
