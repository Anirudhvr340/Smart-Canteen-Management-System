package com.scms.factory;

import com.scms.model.MenuItem;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class MenuItemFactory {

    public MenuItem create(String name,
                           String description,
                           String category,
                           double price,
                           int prepTimeMinutes,
                           Integer caloriesPerServing,
                           String dietaryTags) {
        // Centralize menu item construction so controllers do not own entity wiring.
        return MenuItem.builder()
                .name(name)
                .description(description)
                .category(category)
                .price(price)
                .prepTimeMinutes(prepTimeMinutes)
                .caloriesPerServing(caloriesPerServing)
                .dietaryTags(dietaryTags)
                .build();
    }

    public MenuItem copyOf(MenuItem prototype) {
        // Prototype helper for reusing an existing menu item shape during edits.
        return MenuItem.builder()
                .id(prototype.getId())
                .name(prototype.getName())
                .description(prototype.getDescription())
                .category(prototype.getCategory())
                .price(prototype.getPrice())
                .available(prototype.getAvailable())
                .prepTimeMinutes(prototype.getPrepTimeMinutes())
                .caloriesPerServing(prototype.getCaloriesPerServing())
                .dietaryTags(prototype.getDietaryTags())
                .ingredientQuantities(new HashMap<>(prototype.getIngredientQuantities()))
                .averageRating(prototype.getAverageRating())
                .totalRatings(prototype.getTotalRatings())
                .build();
    }
}