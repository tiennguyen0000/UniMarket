package com.example.unimarket.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.unimarket.data.model.DiscountCode;

import org.junit.Test;

public class DiscountCodeServiceTest {
    private final DiscountCodeService service = new DiscountCodeService();

    @Test
    public void validate_shouldApplyFixedAmountWithinSubtotal() {
        DiscountCode code = new DiscountCode();
        code.setId("discount_student50");
        code.setCode("STUDENT50");
        code.setDiscount_amount(50_000d);
        code.setUsed_count(0);
        code.setMax_uses(10);

        DiscountCodeService.Validation result = service.validate(code, 40_000d);

        assertTrue(result.isValid());
        assertEquals(40_000d, result.getAmount(), 0.001d);
    }

    @Test
    public void validate_shouldRejectWhenUsageLimitReached() {
        DiscountCode code = new DiscountCode();
        code.setCode("WELCOME");
        code.setDiscount_percent(10d);
        code.setUsed_count(3);
        code.setMax_uses(3);

        DiscountCodeService.Validation result = service.validate(code, 200_000d);

        assertFalse(result.isValid());
    }

    @Test
    public void validate_shouldRejectBelowMinimumPurchase() {
        DiscountCode code = new DiscountCode();
        code.setCode("BIGORDER");
        code.setDiscount_percent(10d);
        code.setMin_purchase(500_000d);
        code.setUsed_count(0);
        code.setMax_uses(10);

        DiscountCodeService.Validation result = service.validate(code, 200_000d);

        assertFalse(result.isValid());
    }
}
