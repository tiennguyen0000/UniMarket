package com.example.unimarket.data.model;

public class Report {
    private Long id;
    private Long reporterId;
    private Long productId;
    private String reason;
    private String createdAt;

    public Report() {
    }

    public Report(Long id, Long reporterId, Long productId, String reason, String createdAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.productId = productId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
