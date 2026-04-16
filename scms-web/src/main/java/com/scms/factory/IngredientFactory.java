package com.scms.factory;

import com.scms.model.Ingredient;
import org.springframework.stereotype.Component;

@Component
public class IngredientFactory {

    public Ingredient create(String name,
                             double quantityInStock,
                             double lowStockThreshold,
                             String unit,
                             Double costPerUnit) {
        // Keep ingredient construction consistent across admin and seed workflows.
        return Ingredient.builder()
                .name(name)
                .quantityInStock(quantityInStock)
                .lowStockThreshold(lowStockThreshold)
                .unit(unit)
                .costPerUnit(costPerUnit)
                .build();
    }

    public Ingredient copyOf(Ingredient prototype) {
        // Prototype helper for safe reuse when an ingredient needs a cloned draft.
        return Ingredient.builder()
                .id(prototype.getId())
                .name(prototype.getName())
                .quantityInStock(prototype.getQuantityInStock())
                .reservedQuantity(prototype.getReservedQuantity())
                .lowStockThreshold(prototype.getLowStockThreshold())
                .unit(prototype.getUnit())
                .costPerUnit(prototype.getCostPerUnit())
                .build();
    }
}