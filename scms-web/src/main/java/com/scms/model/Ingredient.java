package com.scms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredients")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Ingredient {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double quantityInStock;

    @Column(nullable = false)
    private Double lowStockThreshold;

    @Column(nullable = false)
    private String unit;

    private Double costPerUnit;

    public boolean isLowStock() {
        return quantityInStock <= lowStockThreshold;
    }

    public void deduct(double amount) {
        if (amount > quantityInStock)
            throw new IllegalStateException("Insufficient stock for: " + name);
        this.quantityInStock -= amount;
    }

    public void restock(double amount) {
        this.quantityInStock += amount;
    }
}
