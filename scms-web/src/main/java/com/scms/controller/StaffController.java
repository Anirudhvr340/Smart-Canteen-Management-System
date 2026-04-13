package com.scms.controller;

import com.scms.model.enums.OrderStatus;
import com.scms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final OrderService orderService;

    @GetMapping("/queue")
    public String queue(Model model) {
        model.addAttribute("activeOrders", orderService.getActiveOrders());
        model.addAttribute("readyOrders",  orderService.getByStatus(OrderStatus.READY));
        model.addAttribute("todayCompleted", orderService.countByStatus(OrderStatus.COMPLETED));
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
}
