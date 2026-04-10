package com.example.unimarket.data.model;

public class Report {
    private String id;
    private String reporter_id;
    private String product_id;
    private String reason;
    private String created_at;

    public Report() {
    }

    public Report(String id, String reporter_id, String product_id, String reason, String created_at) {
        this.id = id;
        this.reporter_id = reporter_id;
        this.product_id = product_id;
        this.reason = reason;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReporter_id() { return reporter_id; }
    public void setReporter_id(String reporter_id) { this.reporter_id = reporter_id; }
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
