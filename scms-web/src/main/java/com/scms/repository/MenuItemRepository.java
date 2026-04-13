package com.scms.repository;
import com.scms.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByAvailableTrue();
    List<MenuItem> findByCategory(String category);
    List<MenuItem> findByNameContainingIgnoreCaseAndAvailableTrue(String keyword);
    List<MenuItem> findAllByOrderByCategory();
}
