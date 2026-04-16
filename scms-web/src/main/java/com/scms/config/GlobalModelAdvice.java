package com.scms.config;

import com.scms.service.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CurrentUserResolver currentUserResolver;

    @ModelAttribute("currentUser")
    public com.scms.model.User currentUser(Authentication auth) {
        return currentUserResolver.resolve(auth);
    }
}
