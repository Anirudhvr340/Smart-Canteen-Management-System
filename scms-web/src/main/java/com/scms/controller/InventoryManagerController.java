package com.scms.controller;

import com.scms.service.InventoryService;
import com.scms.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryManagerController {

    private final InventoryService inventoryService;
    private final MenuService menuService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("ingredients", inventoryService.getAll());
        model.addAttribute("lowStock",    inventoryService.getLowStock());
        model.addAttribute("menuItems",   menuService.getAll());
        return "inventory/dashboard";
    }

    @PostMapping("/{id}/restock")
    public String restock(@PathVariable Long id, @RequestParam double amount, RedirectAttributes ra) {
        inventoryService.restock(id, amount);
        ra.addFlashAttribute("success", "Restocked successfully.");
        return "redirect:/inventory/dashboard";
    }
}
