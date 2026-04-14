package com.scms.repository;
import com.scms.model.MenuItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByAvailableTrue();
    List<MenuItem> findByCategory(String category);
    List<MenuItem> findByNameContainingIgnoreCaseAndAvailableTrue(String keyword);
    List<MenuItem> findAllByOrderByCategory();

    @Query(value = """
            SELECT i.id AS id,
                   i.name AS name,
                   i.unit AS unit,
                   mii.quantity_per_serving AS quantityPerServing
            FROM menu_item_ingredients mii
            JOIN ingredients i ON i.id = mii.ingredient_id
            WHERE mii.menu_item_id = :menuItemId
            ORDER BY i.name
            """, nativeQuery = true)
    List<MenuIngredientRow> findIngredientRowsByMenuItemId(@Param("menuItemId") Long menuItemId);

    interface MenuIngredientRow {
        Long getId();
        String getName();
        String getUnit();
        Double getQuantityPerServing();
    }
}
