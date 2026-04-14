package com.scms.model;

import com.scms.model.enums.OrderStatus;
import com.scms.model.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime scheduledPickupTime;
    private LocalDateTime updatedAt;

    private String couponCode;

    @Builder.Default
    private Double discountApplied = 0.0;

    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Builder.Default
    private Boolean paid = false;

    @Builder.Default
    private Double finalTotal = 0.0;

    @Builder.Default
    private Boolean inventoryReserved = false;

    @Builder.Default
    private Boolean inventoryConsumed = false;

    @Builder.Default
    private Boolean customerCancelRequested = false;

    private String customerCancelReason;

    private String customerNotification;

    public double getRawTotal() {
        return items.stream().mapToDouble(i -> i.getPriceAtOrder() * i.getQuantity()).sum();
    }

    public double computeFinalTotal() {
        double disc = discountApplied != null ? discountApplied : 0;
        return Math.max(0, getRawTotal() - disc);
    }

    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next))
            throw new IllegalStateException("Cannot transition from " + status + " to " + next);
        this.status = next;
        this.updatedAt = LocalDateTime.now();
    }
}
