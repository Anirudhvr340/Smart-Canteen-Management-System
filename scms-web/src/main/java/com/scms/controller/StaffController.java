package com.scms.controller;

import com.scms.model.enums.OrderStatus;
import com.scms.service.OrderService;
import com.scms.service.RequestParameterParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final OrderService orderService;
    private final RequestParameterParser requestParameterParser;

    @GetMapping("/queue")
    public String queue(Model model) {
        model.addAttribute("activeOrders", orderService.getActiveOrders());
        model.addAttribute("readyOrders",  orderService.getByStatus(OrderStatus.READY));
        model.addAttribute("todayCompleted", orderService.countByStatus(OrderStatus.COMPLETED));
        model.addAttribute("cancelRequests", orderService.getCustomerCancelRequests());
        model.addAttribute("cancelRequestIngredients", orderService.getCustomerCancelRequestIngredientOptions());
        return "staff/queue";
    }

    @PostMapping("/order/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes ra) {
        try {
            orderService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Order #" + id + " updated to " + status);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/queue";
    }

    @PostMapping("/order/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue = "Cancelled by staff") String reason,
                              RedirectAttributes ra) {
        try {
            orderService.cancelOrder(id, reason);
            ra.addFlashAttribute("success", "Order #" + id + " cancelled.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/queue";
    }

    @PostMapping("/order/{id}/cancel-request/process")
    public String processCancelRequest(@PathVariable Long id,
                                       @RequestParam Map<String, String> params,
                                       @RequestParam(defaultValue = "Cancelled after customer request") String reason,
                                       RedirectAttributes ra) {
        try {
            // Reuse the same parsing rule across cancellation workflows.
            Map<Long, Double> ingredientQuantities = requestParameterParser
                    .parseSelectedPositiveDoubleMap(params, "returnQty_", "ingredientSelected_");
            orderService.processCustomerCancellationRequest(id, ingredientQuantities, reason);
            ra.addFlashAttribute("success", "Customer cancellation processed for order #" + id + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/queue";
    }
}
