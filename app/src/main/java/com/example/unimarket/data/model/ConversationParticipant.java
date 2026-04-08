package com.example.unimarket.data.model;

public class ConversationParticipant {
    private Long id;
    private Long conversationId;
    private Long userId;

    public ConversationParticipant() {
    }

    public ConversationParticipant(Long id, Long conversationId, Long userId) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
