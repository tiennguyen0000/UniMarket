package com.example.unimarket.data.model;

public class DiscountCode {
    private String id;
    private String code;
    private String description;
    private Double discount_amount;
    private Double discount_percent;
    private Double min_purchase;
    private Integer max_uses;
    private Integer used_count;
    private String valid_until;

    public DiscountCode() {
    }

    public DiscountCode(String id, String code, String description, Double discount_amount,
                       Double discount_percent, Double min_purchase, Integer max_uses,
                       Integer used_count, String valid_until) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.discount_amount = discount_amount;
        this.discount_percent = discount_percent;
        this.min_purchase = min_purchase;
        this.max_uses = max_uses;
        this.used_count = used_count;
        this.valid_until = valid_until;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getDiscount_amount() { return discount_amount; }
    public void setDiscount_amount(Double discount_amount) { this.discount_amount = discount_amount; }

    public Double getDiscount_percent() { return discount_percent; }
    public void setDiscount_percent(Double discount_percent) { this.discount_percent = discount_percent; }

    public Double getMin_purchase() { return min_purchase; }
    public void setMin_purchase(Double min_purchase) { this.min_purchase = min_purchase; }

    public Integer getMax_uses() { return max_uses; }
    public void setMax_uses(Integer max_uses) { this.max_uses = max_uses; }

    public Integer getUsed_count() { return used_count; }
    public void setUsed_count(Integer used_count) { this.used_count = used_count; }

    public String getValid_until() { return valid_until; }
    public void setValid_until(String valid_until) { this.valid_until = valid_until; }
}
