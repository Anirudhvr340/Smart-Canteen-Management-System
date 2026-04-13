package com.scms.service;

import com.scms.model.*;
import com.scms.model.enums.OrderStatus;
import com.scms.repository.FeedbackRepository;
import com.scms.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepo;
    private final MenuItemRepository menuItemRepo;

    @Transactional
    public Feedback submit(User customer, Order order, MenuItem item, double rating, String comment) {
        if (order.getStatus() != OrderStatus.COMPLETED)
            throw new IllegalStateException("Feedback only allowed for completed orders.");
        if (!order.getCustomer().getId().equals(customer.getId()))
            throw new IllegalStateException("Order does not belong to this customer.");

        Feedback fb = Feedback.builder()
                .customer(customer).order(order).menuItem(item)
                .rating(rating).comment(comment).build();
        feedbackRepo.save(fb);

        if (item != null) {
            item.addRating(rating);
            menuItemRepo.save(item);
        }
        return fb;
    }

    public List<Feedback> getAll() { return feedbackRepo.findAll(); }
    public List<Feedback> getByCustomer(Long id) { return feedbackRepo.findByCustomerId(id); }
    public List<Feedback> getByItem(Long id) { return feedbackRepo.findByMenuItemId(id); }
    public List<Feedback> getFlagged() { return feedbackRepo.findByFlaggedTrue(); }

    @Transactional
    public void flag(Long id) {
        feedbackRepo.findById(id).ifPresent(f -> { f.setFlagged(true); feedbackRepo.save(f); });
    }
}
