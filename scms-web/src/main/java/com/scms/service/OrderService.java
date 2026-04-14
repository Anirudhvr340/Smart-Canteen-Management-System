package com.scms.service;

import com.scms.model.*;
import com.scms.model.dto.OrderIngredientUsage;
import com.scms.model.enums.OrderStatus;
import com.scms.model.enums.PaymentMethod;
import com.scms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        order.setCustomerCancelRequested(false);
        order.setCustomerCancelReason(null);
        order.setCustomerNotification("Order was cancelled by staff.");
        orderRepo.save(order);
    }

    @Transactional
    public void requestCustomerCancellation(Long orderId, Long customerId, String reason) {
        Order order = getById(orderId);
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalStateException("You cannot cancel someone else's order.");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Cancellation request allowed only for confirmed or preparing orders.");
        }
        if (Boolean.TRUE.equals(order.getCustomerCancelRequested())) {
            throw new IllegalStateException("Cancellation request already submitted for this order.");
        }

        order.setCustomerCancelRequested(true);
        order.setCustomerCancelReason(reason);
        order.setCustomerNotification("Cancellation request submitted. Waiting for staff review.");
        orderRepo.save(order);
    }

    @Transactional
    public void processCustomerCancellationRequest(Long orderId, Map<Long, Double> ingredientReturnQuantities, String staffReason) {
        Order order = getById(orderId);
        if (!Boolean.TRUE.equals(order.getCustomerCancelRequested())) {
            throw new IllegalStateException("No customer cancellation request found for this order.");
        }

        Map<Long, Double> selectedIngredients = ingredientReturnQuantities != null
                ? new LinkedHashMap<>(ingredientReturnQuantities)
                : new LinkedHashMap<>();

        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            inventoryService.settleReservedForOrder(order, selectedIngredients);
        } else if (Boolean.TRUE.equals(order.getInventoryConsumed())) {
            inventoryService.restockSelectedIngredients(order, selectedIngredients);
        }

        order.setCancellationReason(staffReason != null && !staffReason.isBlank()
                ? staffReason
                : "Cancelled after customer request");
        order.transitionTo(OrderStatus.CANCELLED);
        if (Boolean.TRUE.equals(order.getPaid()) && order.getPaymentMethod() == PaymentMethod.WALLET) {
            User customer = order.getCustomer();
            customer.setWalletBalance(customer.getWalletBalance() + order.getFinalTotal());
        }
        order.setCustomerCancelRequested(false);
        order.setCustomerCancelReason(null);
        order.setCustomerNotification("Cancellation approved by staff. Refund processed where applicable.");
        orderRepo.save(order);
    }

    public Map<Long, List<OrderIngredientUsage>> getCustomerCancelRequestIngredientOptions() {
        Map<Long, List<OrderIngredientUsage>> result = new LinkedHashMap<>();
        for (Order order : getCustomerCancelRequests()) {
            result.put(order.getId(), inventoryService.getIngredientUsage(order));
        }
        return result;
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

    public List<Order> getCustomerCancelRequests() {
        return orderRepo.findByCustomerCancelRequestedTrueOrderByCreatedAtAsc();
    }

    public List<Order> getByStatus(OrderStatus status) {
        return orderRepo.findByStatusOrderByCreatedAtAsc(status);
    }

    public long countByStatus(OrderStatus status) {
        return orderRepo.countByStatus(status);
    }
}
