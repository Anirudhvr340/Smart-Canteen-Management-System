package com.scms.controller;

import com.scms.model.*;
import com.scms.model.enums.PaymentMethod;
import com.scms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;
    private final MenuService menuService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;

    private User currentUser(Authentication auth) {
        return userService.getByEmail(auth.getName());
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    @GetMapping("/menu")
    public String menu(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       Model model, Authentication auth) {
        User user = currentUser(auth);
        List<MenuItem> items = search != null && !search.isBlank()
                ? menuService.searchByName(search)
                : (category != null && !category.isBlank())
                    ? menuService.getByCategory(category)
                    : menuService.getAvailable();

        model.addAttribute("items", items);
        model.addAttribute("user", user);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("categories", List.of("Main Course","Starters","Snacks","Beverages","Desserts"));
        return "customer/menu";
    }

    // ── Place order ───────────────────────────────────────────────────────────

    @PostMapping("/order/place")
    public String placeOrder(@RequestParam Map<String, String> params,
                             Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);

        // Parse itemId_qty params (e.g. item_1=2, item_3=1)
        Map<Long, Integer> itemQtyMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("item_") && !e.getValue().isBlank()) {
                try {
                    int qty = Integer.parseInt(e.getValue().trim());
                    if (qty > 0) {
                        Long itemId = Long.parseLong(e.getKey().replace("item_", ""));
                        itemQtyMap.put(itemId, qty);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (itemQtyMap.isEmpty()) {
            ra.addFlashAttribute("error", "Please add at least one item to your order.");
            return "redirect:/customer/menu";
        }

        String coupon = params.get("couponCode");
        String methodStr = params.getOrDefault("paymentMethod", "WALLET");
        PaymentMethod method = PaymentMethod.valueOf(methodStr);

        try {
            Order order = orderService.placeOrder(user, itemQtyMap, coupon, method, null);
            userService.save(user); // persist wallet deduction
            ra.addFlashAttribute("success",
                    "Order #" + order.getId() + " placed! Total: ₹" + String.format("%.2f", order.getFinalTotal()));
            return "redirect:/customer/orders";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/menu";
        }
    }

    // ── My orders ────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String myOrders(Model model, Authentication auth) {
        User user = currentUser(auth);
        model.addAttribute("orders", orderService.getByCustomer(user.getId()));
        model.addAttribute("user", user);
        return "customer/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Authentication auth) {
        User user = currentUser(auth);
        Order order = orderService.getById(id);
        if (!order.getCustomer().getId().equals(user.getId()))
            return "redirect:/customer/orders";
        model.addAttribute("order", order);
        model.addAttribute("user", user);
        model.addAttribute("feedbacks", feedbackService.getByCustomer(user.getId()));
        return "customer/order-detail";
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    @PostMapping("/feedback")
    public String submitFeedback(@RequestParam Long orderId,
                                 @RequestParam(required = false) Long menuItemId,
                                 @RequestParam double rating,
                                 @RequestParam String comment,
                                 Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);
        Order order = orderService.getById(orderId);
        MenuItem item = menuItemId != null ? menuService.getById(menuItemId) : null;
        try {
            feedbackService.submit(user, order, item, rating, comment);
            ra.addFlashAttribute("success", "Thank you for your feedback!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/orders/" + orderId;
    }

    // ── Wallet ────────────────────────────────────────────────────────────────

    @GetMapping("/wallet")
    public String walletPage(Model model, Authentication auth) {
        model.addAttribute("user", currentUser(auth));
        return "customer/wallet";
    }

    @PostMapping("/wallet/topup")
    public String topUp(@RequestParam double amount, Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);
        if (amount <= 0) { ra.addFlashAttribute("error", "Amount must be positive."); }
        else {
            userService.topUpWallet(user.getId(), amount);
            ra.addFlashAttribute("success", "₹" + amount + " added to wallet!");
        }
        return "redirect:/customer/wallet";
    }
}
