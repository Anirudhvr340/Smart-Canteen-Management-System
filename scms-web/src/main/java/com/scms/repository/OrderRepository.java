package com.scms.repository;
import com.scms.model.Order;
import com.scms.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);
    List<Order> findByCustomerCancelRequestedTrueOrderByCreatedAtAsc();
    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.finalTotal), 0.0) FROM Order o WHERE o.paid = true AND o.createdAt >= :from AND o.createdAt < :to")
    Double sumRevenueByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to")
    Long countByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :s AND o.createdAt >= :from AND o.createdAt < :to")
    Long countByStatusAndDateRange(@Param("s") OrderStatus s, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
