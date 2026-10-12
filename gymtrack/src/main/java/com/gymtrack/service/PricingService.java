package com.gymtrack.service;

import com.gymtrack.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Single place for the gym's pricing rule.
 * Base rate is Rs 1000 per month; longer plans get a discount:
 *   1-2 months  -> no discount
 *   3-11 months -> 10% off
 *   12+ months  -> 20% off
 */
@Service
public class PricingService {

    public static final BigDecimal BASE_MONTHLY_RATE = new BigDecimal("1000");

    public BigDecimal discountFor(int months) {
        if (months >= 12) return new BigDecimal("0.20");
        if (months >= 3) return new BigDecimal("0.10");
        return BigDecimal.ZERO;
    }

    public BigDecimal priceFor(int months) {
        if (months < 1) {
            throw new BusinessRuleException("Duration must be at least 1 month");
        }
        BigDecimal total = BASE_MONTHLY_RATE.multiply(BigDecimal.valueOf(months));
        BigDecimal discount = total.multiply(discountFor(months));
        return total.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }
}