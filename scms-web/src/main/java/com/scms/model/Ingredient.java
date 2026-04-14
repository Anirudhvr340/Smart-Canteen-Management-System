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

    @Builder.Default
    @Column(nullable = false)
    private Double reservedQuantity = 0.0;

    @Column(nullable = false)
    private Double lowStockThreshold;

    @Column(nullable = false)
    private String unit;

    private Double costPerUnit;

    public boolean isLowStock() {
        return getAvailableQuantity() <= lowStockThreshold;
    }

    public double getAvailableQuantity() {
        return quantityInStock - (reservedQuantity != null ? reservedQuantity : 0.0);
    }

    public void deduct(double amount) {
        if (amount > quantityInStock)
            throw new IllegalStateException("Insufficient stock for: " + name);
        this.quantityInStock -= amount;
    }

    public void reserve(double amount) {
        if (amount > getAvailableQuantity())
            throw new IllegalStateException("Insufficient available stock for: " + name);
        this.reservedQuantity += amount;
    }

    public void releaseReserved(double amount) {
        this.reservedQuantity = Math.max(0.0, this.reservedQuantity - amount);
    }

    public void consumeReserved(double amount) {
        if (amount > reservedQuantity)
            throw new IllegalStateException("Reserved stock mismatch for: " + name);
        this.reservedQuantity -= amount;
        deduct(amount);
    }

    public void restock(double amount) {
        this.quantityInStock += amount;
    }
}
