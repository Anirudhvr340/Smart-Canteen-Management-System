package com.scms.factory;

import com.scms.model.MenuItem;
import com.scms.model.Order;
import com.scms.model.OrderItem;
import com.scms.model.User;
import com.scms.model.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderFactory {

    // Singleton-scoped Spring bean that centralizes order object creation.
    public Order createOrder(User customer, PaymentMethod paymentMethod, LocalDateTime pickupTime) {
        return Order.builder()
                .customer(customer)
                .scheduledPickupTime(pickupTime != null ? pickupTime : LocalDateTime.now().plusMinutes(15))
                .paymentMethod(paymentMethod)
                .build();
    }

    public OrderItem createOrderItem(Order order, MenuItem menuItem, Integer quantity) {
        return OrderItem.builder()
                .order(order)
                .menuItem(menuItem)
                .quantity(quantity)
                .priceAtOrder(menuItem.getPrice())
                .build();
    }

    // Prototype-style copy for scenarios where a previous order needs to be duplicated.
    public OrderItem copyItemForOrder(OrderItem prototype, Order targetOrder) {
        return prototype.copyFor(targetOrder);
    }
}