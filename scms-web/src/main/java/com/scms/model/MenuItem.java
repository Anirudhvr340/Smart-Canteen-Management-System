package com.scms.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "menu_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    private String category;

    @Column(nullable = false)
    private Double price;

    @Builder.Default
    private Boolean available = true;

    private Integer prepTimeMinutes;
    private Integer caloriesPerServing;
    private String dietaryTags;   // comma-separated: VEG,GLUTEN_FREE

    // Ingredient recipe: ingredient_id -> qty per serving
    @ElementCollection
    @CollectionTable(name = "menu_item_ingredients",
                     joinColumns = @JoinColumn(name = "menu_item_id"))
    @MapKeyColumn(name = "ingredient_id")
    @Column(name = "quantity_per_serving")
    @Builder.Default
    private Map<Long, Double> ingredientQuantities = new HashMap<>();

    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalRatings = 0;

    public void addRating(double rating) {
        totalRatings++;
        averageRating = ((averageRating * (totalRatings - 1)) + rating) / totalRatings;
    }
}
