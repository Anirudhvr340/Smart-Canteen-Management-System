package com.scms.factory;

import com.scms.model.Coupon;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CouponFactory {

    public Coupon create(String code,
                         Coupon.Type type,
                         double value,
                         double minOrderValue,
                         double maxDiscount,
                         int usageLimit,
                         int expiryDays) {
        // Encapsulate coupon construction and date handling in one place.
        return Coupon.builder()
                .code(code.toUpperCase())
                .type(type)
                .value(value)
                .minOrderValue(minOrderValue)
                .maxDiscount(maxDiscount)
                .usageLimit(usageLimit)
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .build();
    }

    public Coupon copyOf(Coupon prototype) {
        // Prototype helper for cloning the current coupon state into a new draft.
        return Coupon.builder()
                .id(prototype.getId())
                .code(prototype.getCode())
                .type(prototype.getType())
                .value(prototype.getValue())
                .minOrderValue(prototype.getMinOrderValue())
                .maxDiscount(prototype.getMaxDiscount())
                .usageLimit(prototype.getUsageLimit())
                .usedCount(prototype.getUsedCount())
                .expiresAt(prototype.getExpiresAt())
                .active(prototype.getActive())
                .build();
    }
}