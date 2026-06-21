package com.example.unimarket.data.model;

public class SavedSearch {
    private String id;
    private String user_id;
    private String name;
    private String query;
    private String category_id;
    private Double min_price;
    private Double max_price;
    private boolean filter_new;
    private boolean filter_used;
    private boolean filter_saved_only;
    private String sort;
    private boolean alerts_enabled;
    private String last_seen_product_created_at;
    private String created_at;
    private String updated_at;

    public SavedSearch() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getCategory_id() { return category_id; }
    public void setCategory_id(String category_id) { this.category_id = category_id; }
    public Double getMin_price() { return min_price; }
    public void setMin_price(Double min_price) { this.min_price = min_price; }
    public Double getMax_price() { return max_price; }
    public void setMax_price(Double max_price) { this.max_price = max_price; }
    public boolean isFilter_new() { return filter_new; }
    public void setFilter_new(boolean filter_new) { this.filter_new = filter_new; }
    public boolean isFilter_used() { return filter_used; }
    public void setFilter_used(boolean filter_used) { this.filter_used = filter_used; }
    public boolean isFilter_saved_only() { return filter_saved_only; }
    public void setFilter_saved_only(boolean filter_saved_only) { this.filter_saved_only = filter_saved_only; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public boolean isAlerts_enabled() { return alerts_enabled; }
    public void setAlerts_enabled(boolean alerts_enabled) { this.alerts_enabled = alerts_enabled; }
    public String getLast_seen_product_created_at() { return last_seen_product_created_at; }
    public void setLast_seen_product_created_at(String last_seen_product_created_at) {
        this.last_seen_product_created_at = last_seen_product_created_at;
    }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
