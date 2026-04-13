package com.scms.service;

import com.scms.model.User;
import com.scms.model.enums.Role;
import com.scms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }

    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    public User getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public List<User> getAll() { return userRepo.findAll(); }

    public List<User> getByRole(Role role) { return userRepo.findByRole(role); }

    @Transactional
    public User register(String name, String email, String rawPassword, Role role) {
        if (userRepo.existsByEmail(email))
            throw new IllegalStateException("Email already registered: " + email);
        User u = User.builder()
                .name(name).email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role).build();
        return userRepo.save(u);
    }

    @Transactional
    public void topUpWallet(Long userId, double amount) {
        User u = getById(userId);
        u.setWalletBalance(u.getWalletBalance() + amount);
        userRepo.save(u);
    }

    @Transactional
    public void recordLogin(String email) {
        userRepo.findByEmail(email).ifPresent(u -> {
            u.setLastLogin(LocalDateTime.now());
            userRepo.save(u);
        });
    }

    @Transactional
    public void toggleActive(Long id) {
        User u = getById(id);
        u.setActive(!u.getActive());
        userRepo.save(u);
    }

    @Transactional
    public User save(User u) { return userRepo.save(u); }
}
