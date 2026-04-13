package com.scms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

    public enum Type { PERCENTAGE, FLAT_AMOUNT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    private Type type;

    private Double value;
    private Double minOrderValue;
    private Double maxDiscount;
    private Integer usageLimit;

    @Builder.Default
    private Integer usedCount = 0;

    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean active = true;

    public boolean isValid(double orderTotal) {
        return Boolean.TRUE.equals(active)
            && usedCount < usageLimit
            && LocalDateTime.now().isBefore(expiresAt)
            && orderTotal >= minOrderValue;
    }

    public double computeDiscount(double orderTotal) {
        if (!isValid(orderTotal)) return 0;
        if (type == Type.PERCENTAGE) return Math.min(orderTotal * value / 100.0, maxDiscount);
        return Math.min(value, orderTotal);
    }

    public void redeem() { usedCount++; }
}
