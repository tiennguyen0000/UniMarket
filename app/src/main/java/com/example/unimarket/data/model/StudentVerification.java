package com.example.unimarket.data.model;

public class StudentVerification {
    private String id;
    private String user_id;
    private String method;
    private String status;
    private String proof_url;
    private String front_card_url;
    private String back_card_url;
    private String user_name;
    private String student_id;
    private String note;
    private String created_at;
    private String reviewed_at;

    public StudentVerification() {
    }

    public StudentVerification(String id, String user_id, String method, String status, String proof_url, String created_at) {
        this.id = id;
        this.user_id = user_id;
        this.method = method;
        this.status = status;
        this.proof_url = proof_url;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProof_url() { return proof_url; }
    public void setProof_url(String proof_url) { this.proof_url = proof_url; }
    public String getFront_card_url() { return front_card_url; }
    public void setFront_card_url(String front_card_url) { this.front_card_url = front_card_url; }
    public String getBack_card_url() { return back_card_url; }
    public void setBack_card_url(String back_card_url) { this.back_card_url = back_card_url; }
    public String getUser_name() { return user_name; }
    public void setUser_name(String user_name) { this.user_name = user_name; }
    public String getStudent_id() { return student_id; }
    public void setStudent_id(String student_id) { this.student_id = student_id; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getReviewed_at() { return reviewed_at; }
    public void setReviewed_at(String reviewed_at) { this.reviewed_at = reviewed_at; }
}
