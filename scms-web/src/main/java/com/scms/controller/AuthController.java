package com.scms.controller;

import com.scms.model.enums.Role;
import com.scms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String root(Authentication auth) {
        return auth != null ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid email or password.");
        if (logout != null) model.addAttribute("msg", "Logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("roles", new Role[]{Role.CUSTOMER, Role.STAFF, Role.INVENTORY_MANAGER, Role.ADMIN});
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam Role role,
                             RedirectAttributes ra) {
        try {
            userService.register(name, email, password, role);
            ra.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/login";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")))
            return "redirect:/admin/dashboard";
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STAFF")))
            return "redirect:/staff/queue";
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_INVENTORY_MANAGER")))
            return "redirect:/inventory/dashboard";
        return "redirect:/customer/menu";
    }
}
