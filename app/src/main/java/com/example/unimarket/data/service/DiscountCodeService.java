package com.example.unimarket.data.service;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.DiscountCode;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.service.base.BaseCrudService;
import com.example.unimarket.data.service.base.Result;
import com.example.unimarket.data.service.base.ResultCallback;
import com.example.unimarket.data.util.TimeUtils;

import java.util.List;
import java.util.Locale;

public class DiscountCodeService extends BaseCrudService<DiscountCode> {

    public void validateCode(String rawCode, double subtotal, ResultCallback<Validation> callback) {
        String normalizedCode = rawCode != null ? rawCode.trim().toUpperCase(Locale.ROOT) : "";
        if (normalizedCode.isEmpty()) {
            if (callback != null) callback.onResult(Result.error("Vui lòng nhập mã giảm giá."));
            return;
        }

        getWithFilter("code", normalizedCode, new AsyncCrudService.ListCallback<DiscountCode>() {
            @Override
            public void onSuccess(List<DiscountCode> data) {
                DiscountCode code = data != null && !data.isEmpty() ? data.get(0) : null;
                Validation validation = validate(code, subtotal);
                if (callback == null) return;
                if (validation.isValid()) {
                    callback.onResult(Result.success(validation));
                } else {
                    callback.onResult(Result.error(validation.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onResult(Result.error(error));
            }
        });
    }

    public Validation validate(DiscountCode code, double subtotal) {
        if (code == null) {
            return Validation.invalid("Mã không hợp lệ hoặc đã hết hạn.");
        }
        if (TimeUtils.isIsoUtcExpired(code.getValid_until())) {
            return Validation.invalid("Mã giảm giá đã hết hạn.");
        }

        int usedCount = code.getUsed_count() != null ? code.getUsed_count() : 0;
        int maxUses = code.getMax_uses() != null ? code.getMax_uses() : Integer.MAX_VALUE;
        if (usedCount >= maxUses) {
            return Validation.invalid("Mã giảm giá đã hết lượt sử dụng.");
        }

        double minPurchase = code.getMin_purchase() != null ? code.getMin_purchase() : 0d;
        if (subtotal < minPurchase) {
            return Validation.invalid("Đơn hàng chưa đạt giá trị tối thiểu cho mã này.");
        }

        double amount = code.getDiscount_amount() != null ? code.getDiscount_amount() : 0d;
        if (code.getDiscount_percent() != null && code.getDiscount_percent() > 0) {
            amount += subtotal * (code.getDiscount_percent() / 100d);
        }
        amount = Math.max(0d, Math.min(amount, subtotal));
        if (amount <= 0d) {
            return Validation.invalid("Mã giảm giá chưa có giá trị áp dụng.");
        }

        return Validation.valid(code, amount);
    }

    @Override
    public String getId(DiscountCode item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(DiscountCode item, String id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return DomainConstants.Collections.DISCOUNT_CODES;
    }

    @Override
    protected Class<DiscountCode> getModelClass() {
        return DiscountCode.class;
    }

    public static final class Validation {
        private final boolean valid;
        private final DiscountCode discountCode;
        private final double amount;
        private final String message;

        private Validation(boolean valid, DiscountCode discountCode, double amount, String message) {
            this.valid = valid;
            this.discountCode = discountCode;
            this.amount = amount;
            this.message = message;
        }

        public static Validation valid(DiscountCode discountCode, double amount) {
            return new Validation(true, discountCode, amount, "Mã giảm giá hợp lệ.");
        }

        public static Validation invalid(String message) {
            return new Validation(false, null, 0d, message);
        }

        public boolean isValid() { return valid; }
        public DiscountCode getDiscountCode() { return discountCode; }
        public double getAmount() { return amount; }
        public String getMessage() { return message; }
    }
}
