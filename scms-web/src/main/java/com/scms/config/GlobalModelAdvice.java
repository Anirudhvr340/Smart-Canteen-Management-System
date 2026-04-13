package com.scms.config;

import com.scms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserService userService;

    @ModelAttribute("currentUser")
    public com.scms.model.User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        try { return userService.getByEmail(auth.getName()); }
        catch (Exception e) { return null; }
    }
}
