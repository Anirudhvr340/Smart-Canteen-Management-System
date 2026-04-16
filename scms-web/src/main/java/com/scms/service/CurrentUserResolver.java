package com.scms.service;

import com.scms.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserService userService;

    public User resolve(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        // Keep authentication-to-domain lookup in one place.
        try {
            return userService.getByEmail(auth.getName());
        } catch (Exception ex) {
            return null;
        }
    }
}