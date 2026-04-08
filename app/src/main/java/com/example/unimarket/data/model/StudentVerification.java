package com.example.unimarket.data.model;

public class StudentVerification {
    private Long id;
    private Long userId;
    private String method;
    private String status;
    private String proofUrl;
    private String createdAt;

    public StudentVerification() {
    }

    public StudentVerification(Long id, Long userId, String method, String status, String proofUrl, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.method = method;
        this.status = status;
        this.proofUrl = proofUrl;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
