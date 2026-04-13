package com.scms.controller;

import com.scms.model.*;
import com.scms.model.enums.Role;
import com.scms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MenuService menuService;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final UserService userService;
    private final FeedbackService feedbackService;
    private final AnalyticsService analyticsService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("todayRevenue",    analyticsService.getTodayRevenue());
        model.addAttribute("monthRevenue",    analyticsService.getMonthRevenue());
        model.addAttribute("todayOrders",     analyticsService.getTodayOrderCount());
        model.addAttribute("todayCancels",    analyticsService.getTodayCancelCount());
        model.addAttribute("activeOrders",    orderService.getActiveOrders().size());
        model.addAttribute("lowStock",        inventoryService.getLowStock().size());
        model.addAttribute("revenueChart",    analyticsService.getLast7DaysRevenue());
        model.addAttribute("statusChart",     analyticsService.getOrdersByStatus());
        model.addAttribute("recentOrders",    orderService.getAllOrders().stream().limit(10).toList());
        return "admin/dashboard";
    }

    // ── Menu management ───────────────────────────────────────────────────────

    @GetMapping("/menu")
    public String menuList(Model model) {
        model.addAttribute("items", menuService.getAll());
        model.addAttribute("ingredients", inventoryService.getAll());
        return "admin/menu";
    }

    @PostMapping("/menu/add")
    public String addItem(@RequestParam String name, @RequestParam String description,
                          @RequestParam String category,  @RequestParam double price,
                          @RequestParam int prepTimeMinutes, @RequestParam(required=false) Integer caloriesPerServing,
                          @RequestParam(required=false) String dietaryTags,
                          RedirectAttributes ra) {
        MenuItem item = MenuItem.builder()
                .name(name).description(description).category(category)
                .price(price).prepTimeMinutes(prepTimeMinutes)
                .caloriesPerServing(caloriesPerServing)
                .dietaryTags(dietaryTags).build();
        menuService.save(item);
        ra.addFlashAttribute("success", "Menu item added: " + name);
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/{id}/toggle")
    public String toggleItem(@PathVariable Long id, RedirectAttributes ra) {
        menuService.toggleAvailability(id);
        ra.addFlashAttribute("success", "Availability toggled.");
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/{id}/delete")
    public String deleteItem(@PathVariable Long id, RedirectAttributes ra) {
        menuService.delete(id);
        ra.addFlashAttribute("success", "Item deleted.");
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/{id}/ingredient")
    public String mapIngredient(@PathVariable Long id,
                                @RequestParam Long ingredientId,
                                @RequestParam double quantity,
                                RedirectAttributes ra) {
        menuService.mapIngredient(id, ingredientId, quantity);
        ra.addFlashAttribute("success", "Ingredient mapped.");
        return "redirect:/admin/menu";
    }

    // ── Inventory management ──────────────────────────────────────────────────

    @GetMapping("/inventory")
    public String inventoryList(Model model) {
        model.addAttribute("ingredients", inventoryService.getAll());
        model.addAttribute("lowStock",    inventoryService.getLowStock());
        return "admin/inventory";
    }

    @PostMapping("/inventory/add")
    public String addIngredient(@RequestParam String name, @RequestParam double quantityInStock,
                                @RequestParam double lowStockThreshold, @RequestParam String unit,
                                @RequestParam(required=false, defaultValue="0") double costPerUnit, RedirectAttributes ra) {
        Ingredient ing = Ingredient.builder().name(name).quantityInStock(quantityInStock)
                .lowStockThreshold(lowStockThreshold).unit(unit).costPerUnit(costPerUnit).build();
        inventoryService.save(ing);
        ra.addFlashAttribute("success", "Ingredient added: " + name);
        return "redirect:/admin/inventory";
    }

    @PostMapping("/inventory/{id}/restock")
    public String restock(@PathVariable Long id, @RequestParam double amount, RedirectAttributes ra) {
        inventoryService.restock(id, amount);
        ra.addFlashAttribute("success", "Restocked successfully.");
        return "redirect:/admin/inventory";
    }

    @PostMapping("/inventory/{id}/delete")
    public String deleteIngredient(@PathVariable Long id, RedirectAttributes ra) {
        inventoryService.delete(id);
        ra.addFlashAttribute("success", "Ingredient deleted.");
        return "redirect:/admin/inventory";
    }

    // ── Coupon management ─────────────────────────────────────────────────────

    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("coupons", couponService.getAll());
        return "admin/coupons";
    }

    @PostMapping("/coupons/add")
    public String addCoupon(@RequestParam String code, @RequestParam Coupon.Type type,
                            @RequestParam double value, @RequestParam double minOrderValue,
                            @RequestParam double maxDiscount, @RequestParam int usageLimit,
                            @RequestParam int expiryDays, RedirectAttributes ra) {
        Coupon c = Coupon.builder().code(code.toUpperCase()).type(type).value(value)
                .minOrderValue(minOrderValue).maxDiscount(maxDiscount).usageLimit(usageLimit)
                .expiresAt(LocalDateTime.now().plusDays(expiryDays)).build();
        couponService.save(c);
        ra.addFlashAttribute("success", "Coupon created: " + code);
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/deactivate")
    public String deactivateCoupon(@PathVariable Long id, RedirectAttributes ra) {
        couponService.deactivate(id);
        ra.addFlashAttribute("success", "Coupon deactivated.");
        return "redirect:/admin/coupons";
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String allOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue="Admin cancelled") String reason,
                              RedirectAttributes ra) {
        try { orderService.cancelOrder(id, reason); ra.addFlashAttribute("success","Order cancelled."); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/orders";
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleActive(id);
        ra.addFlashAttribute("success","User status toggled.");
        return "redirect:/admin/users";
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    @GetMapping("/feedback")
    public String feedback(Model model) {
        model.addAttribute("feedbacks", feedbackService.getAll());
        model.addAttribute("flagged",   feedbackService.getFlagged());
        return "admin/feedback";
    }

    @PostMapping("/feedback/{id}/flag")
    public String flagFeedback(@PathVariable Long id, RedirectAttributes ra) {
        feedbackService.flag(id);
        ra.addFlashAttribute("success","Feedback flagged.");
        return "redirect:/admin/feedback";
    }
}
