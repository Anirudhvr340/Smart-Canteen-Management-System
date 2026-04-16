package com.scms.controller;

import com.scms.factory.CouponFactory;
import com.scms.factory.IngredientFactory;
import com.scms.factory.MenuItemFactory;
import com.scms.model.*;
import com.scms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

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
    private final MenuItemFactory menuItemFactory;
    private final IngredientFactory ingredientFactory;
    private final CouponFactory couponFactory;
    private final RequestParameterParser requestParameterParser;

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
                          @RequestParam(required=false) List<String> ingredientId,
                          @RequestParam(required=false) List<String> ingredientQuantity,
                          @RequestParam(required=false) List<String> newIngredientName,
                          @RequestParam(required=false) List<String> newIngredientUnit,
                          @RequestParam(required=false) List<String> newIngredientQuantityInStock,
                          @RequestParam(required=false) List<String> newIngredientLowStockThreshold,
                          @RequestParam(required=false) List<String> newIngredientCostPerUnit,
                          @RequestParam(required=false) List<String> newIngredientRecipeQty,
                          RedirectAttributes ra) {
                MenuItem item = menuItemFactory.create(
                    name,
                    description,
                    category,
                    price,
                    prepTimeMinutes,
                    caloriesPerServing,
                    dietaryTags
                );
        item = menuService.save(item);

        int mappedCount = processIngredientMappings(
                item,
                ingredientId,
                ingredientQuantity,
                newIngredientName,
                newIngredientUnit,
                newIngredientQuantityInStock,
                newIngredientLowStockThreshold,
                newIngredientCostPerUnit,
                newIngredientRecipeQty
        );

        inventoryService.refreshMenuAvailability();
        ra.addFlashAttribute("success", "Menu item added: " + name + (mappedCount > 0 ? " (" + mappedCount + " ingredients mapped)" : ""));
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/{id}/edit")
    public String editItem(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam String description,
                           @RequestParam String category,
                           @RequestParam double price,
                           @RequestParam int prepTimeMinutes,
                           @RequestParam(required=false) Integer caloriesPerServing,
                           @RequestParam(required=false) String dietaryTags,
                           @RequestParam(required=false) List<String> ingredientId,
                           @RequestParam(required=false) List<String> ingredientQuantity,
                           @RequestParam(required=false) List<String> newIngredientName,
                           @RequestParam(required=false) List<String> newIngredientUnit,
                           @RequestParam(required=false) List<String> newIngredientQuantityInStock,
                           @RequestParam(required=false) List<String> newIngredientLowStockThreshold,
                           @RequestParam(required=false) List<String> newIngredientCostPerUnit,
                           @RequestParam(required=false) List<String> newIngredientRecipeQty,
                           RedirectAttributes ra) {
                MenuItem item = menuItemFactory.copyOf(menuService.getById(id));
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setPrice(price);
        item.setPrepTimeMinutes(prepTimeMinutes);
        item.setCaloriesPerServing(caloriesPerServing);
        item.setDietaryTags(dietaryTags);

        // Replace existing recipe with submitted one.
        item.getIngredientQuantities().clear();
        item = menuService.save(item);

        int mappedCount = processIngredientMappings(
                item,
                ingredientId,
                ingredientQuantity,
                newIngredientName,
                newIngredientUnit,
                newIngredientQuantityInStock,
                newIngredientLowStockThreshold,
                newIngredientCostPerUnit,
                newIngredientRecipeQty
        );

        inventoryService.refreshMenuAvailability();
        ra.addFlashAttribute("success", "Menu item updated: " + name + (mappedCount > 0 ? " (" + mappedCount + " ingredients mapped)" : ""));
        return "redirect:/admin/menu";
    }

    @GetMapping("/menu/{id}/edit-data")
    @ResponseBody
    public MenuItemEditView getMenuItemEditData(@PathVariable Long id) {
        MenuItem item = menuService.getById(id);
        List<MenuIngredientView> ingredientViews = menuService.getIngredientInfo(id)
                .stream()
                .map(info -> new MenuIngredientView(
                        info.id(),
                        info.name(),
                        info.unit(),
                        info.quantityPerServing()
                ))
                .toList();

        return new MenuItemEditView(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getPrice(),
                item.getPrepTimeMinutes(),
                item.getCaloriesPerServing(),
                item.getDietaryTags(),
                ingredientViews
        );
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

    @GetMapping("/menu/{id}/ingredients")
    @ResponseBody
    public List<MenuIngredientView> getItemIngredients(@PathVariable Long id) {
        menuService.getById(id);
        return menuService.getIngredientInfo(id)
            .stream()
            .map(info -> new MenuIngredientView(
                info.id(),
                info.name(),
                info.unit(),
                info.quantityPerServing()
            ))
            .toList();
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
        Ingredient ing = ingredientFactory.create(name, quantityInStock, lowStockThreshold, unit, costPerUnit);
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
        Coupon c = couponFactory.create(code, type, value, minOrderValue, maxDiscount, usageLimit, expiryDays);
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

    private int processIngredientMappings(MenuItem item,
                                          List<String> ingredientId,
                                          List<String> ingredientQuantity,
                                          List<String> newIngredientName,
                                          List<String> newIngredientUnit,
                                          List<String> newIngredientQuantityInStock,
                                          List<String> newIngredientLowStockThreshold,
                                          List<String> newIngredientCostPerUnit,
                                          List<String> newIngredientRecipeQty) {
        int mappedCount = 0;

        if (ingredientId != null && ingredientQuantity != null) {
            int count = Math.min(ingredientId.size(), ingredientQuantity.size());
            for (int i = 0; i < count; i++) {
                Long ingId = requestParameterParser.parseLongOrNull(ingredientId.get(i));
                Double qty = requestParameterParser.parseDoubleOrNull(ingredientQuantity.get(i));
                if (ingId != null && qty != null && qty > 0) {
                    menuService.mapIngredient(item.getId(), ingId, qty);
                    mappedCount++;
                }
            }
        }

        if (newIngredientName != null) {
            int count = newIngredientName.size();
            for (int i = 0; i < count; i++) {
                String ingName = requestParameterParser.getListValue(newIngredientName, i);
                if (ingName == null || ingName.isBlank()) {
                    continue;
                }

                String unit = requestParameterParser.getListValue(newIngredientUnit, i);
                if (unit == null || unit.isBlank()) {
                    unit = "g";
                }

                double quantityInStock = Math.max(0.0, requestParameterParser.parseDoubleOrDefault(requestParameterParser.getListValue(newIngredientQuantityInStock, i), 0.0));
                double lowStockThreshold = Math.max(0.0, requestParameterParser.parseDoubleOrDefault(requestParameterParser.getListValue(newIngredientLowStockThreshold, i), 0.0));
                Double costPerUnit = requestParameterParser.parseDoubleOrNull(requestParameterParser.getListValue(newIngredientCostPerUnit, i));
                double recipeQty = requestParameterParser.parseDoubleOrDefault(requestParameterParser.getListValue(newIngredientRecipeQty, i), 0.0);

                Ingredient ingredient = ingredientFactory.create(
                        ingName.trim(),
                        quantityInStock,
                        lowStockThreshold,
                        unit,
                        costPerUnit
                );

                ingredient = inventoryService.save(ingredient);

                if (recipeQty > 0) {
                    menuService.mapIngredient(item.getId(), ingredient.getId(), recipeQty);
                    mappedCount++;
                }
            }
        }

        return mappedCount;
    }

    private record MenuIngredientView(Long id, String name, String unit, Double quantityPerServing) {}

    private record MenuItemEditView(Long id,
                                    String name,
                                    String description,
                                    String category,
                                    Double price,
                                    Integer prepTimeMinutes,
                                    Integer caloriesPerServing,
                                    String dietaryTags,
                                    List<MenuIngredientView> ingredients) {}
}
