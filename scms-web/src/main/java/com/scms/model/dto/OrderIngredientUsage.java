package com.scms.model.dto;

import com.scms.model.Ingredient;

public record OrderIngredientUsage(Ingredient ingredient, double quantity) {
}
