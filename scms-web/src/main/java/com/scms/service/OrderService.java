package com.scms.service;

import com.scms.model.*;
import com.scms.model.enums.OrderStatus;
import com.scms.model.enums.PaymentMethod;
import com.scms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final MenuService menuService;
    private final CouponService couponService;
    private final InventoryService inventoryService;

    // ── Cart / placement ─────────────────────────────────────────────────────

    @Transactional
    public Order placeOrder(User customer,
                            Map<Long, Integer> itemQtyMap,
                            String couponCode,
                            PaymentMethod paymentMethod,
                            LocalDateTime pickupTime) {

        Order order = Order.builder()
                .customer(customer)
                .scheduledPickupTime(pickupTime != null ? pickupTime : LocalDateTime.now().plusMinutes(15))
                .paymentMethod(paymentMethod)
                .build();

        // Add items
        for (Map.Entry<Long, Integer> entry : itemQtyMap.entrySet()) {
            MenuItem mi = menuService.getById(entry.getKey());
            if (!mi.getAvailable())
                throw new IllegalStateException(mi.getName() + " is currently unavailable.");

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .menuItem(mi)
                    .quantity(entry.getValue())
                    .priceAtOrder(mi.getPrice())
                    .build();
            order.getItems().add(oi);
        }

        // Reserve stock immediately to prevent overselling while order is queued.
        inventoryService.reserveForOrder(order);
        order.setInventoryReserved(true);

        // Apply coupon
        double rawTotal = order.getRawTotal();
        double discount = 0;
        if (couponCode != null && !couponCode.isBlank()) {
            discount = couponService.applyAndRedeem(couponCode.trim(), rawTotal);
            order.setCouponCode(couponCode.trim());
        }
        order.setDiscountApplied(discount);
        double finalTotal = order.computeFinalTotal();
        order.setFinalTotal(finalTotal);

        // Handle wallet payment
        if (paymentMethod == PaymentMethod.WALLET) {
            if (customer.getWalletBalance() < finalTotal)
                throw new IllegalStateException("Insufficient wallet balance. Top up and try again.");
            customer.setWalletBalance(customer.getWalletBalance() - finalTotal);
        }

        order.setPaid(paymentMethod != PaymentMethod.CASH);
        order.transitionTo(OrderStatus.CONFIRMED);
        return orderRepo.save(order);
    }

    // ── Status transitions ───────────────────────────────────────────────────

    @Transactional
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = getById(orderId);
        order.transitionTo(newStatus);
        if (newStatus == OrderStatus.PREPARING) {
            inventoryService.consumeReservedForOrder(order);
        }
        orderRepo.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = getById(orderId);
        order.setCancellationReason(reason);
        order.transitionTo(OrderStatus.CANCELLED);

        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            inventoryService.releaseReservationForOrder(order);
        }

        // Refund wallet if paid by wallet
        if (Boolean.TRUE.equals(order.getPaid()) && order.getPaymentMethod() == PaymentMethod.WALLET) {
            User customer = order.getCustomer();
            customer.setWalletBalance(customer.getWalletBalance() + order.getFinalTotal());
        }
        orderRepo.save(order);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public Order getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    public List<Order> getByCustomer(Long customerId) {
        return orderRepo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Order> getActiveOrders() {
        return orderRepo.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING));
    }

    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }

    public List<Order> getByStatus(OrderStatus status) {
        return orderRepo.findByStatusOrderByCreatedAtAsc(status);
    }

    public long countByStatus(OrderStatus status) {
        return orderRepo.countByStatus(status);
    }
}
