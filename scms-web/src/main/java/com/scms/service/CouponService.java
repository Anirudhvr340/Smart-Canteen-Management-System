package com.scms.service;

import com.scms.model.Coupon;
import com.scms.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepo;

    public List<Coupon> getAll() { return couponRepo.findAll(); }

    public Optional<Coupon> findByCode(String code) {
        return couponRepo.findByCodeIgnoreCase(code);
    }

    @Transactional
    public Coupon save(Coupon coupon) { return couponRepo.save(coupon); }

    @Transactional
    public double applyAndRedeem(String code, double orderTotal) {
        return couponRepo.findByCodeIgnoreCase(code).map(c -> {
            double disc = c.computeDiscount(orderTotal);
            if (disc > 0) { c.redeem(); couponRepo.save(c); }
            return disc;
        }).orElse(0.0);
    }

    @Transactional
    public void deactivate(Long id) {
        couponRepo.findById(id).ifPresent(c -> { c.setActive(false); couponRepo.save(c); });
    }

    @Transactional
    public void delete(Long id) { couponRepo.deleteById(id); }
}
