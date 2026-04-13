package com.scms.repository;
import com.scms.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByMenuItemId(Long menuItemId);
    List<Feedback> findByCustomerId(Long customerId);
    List<Feedback> findByFlaggedTrue();
}
