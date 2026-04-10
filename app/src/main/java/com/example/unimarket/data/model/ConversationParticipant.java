package com.example.unimarket.data.model;

public class ConversationParticipant {
    private String id;
    private String conversation_id;
    private String user_id;

    public ConversationParticipant() {
    }

    public ConversationParticipant(String id, String conversation_id, String user_id) {
        this.id = id;
        this.conversation_id = conversation_id;
        this.user_id = user_id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversation_id() { return conversation_id; }
    public void setConversation_id(String conversation_id) { this.conversation_id = conversation_id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
}
