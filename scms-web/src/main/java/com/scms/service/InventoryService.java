package com.scms.service;

import com.scms.model.Ingredient;
import com.scms.model.MenuItem;
import com.scms.model.Order;
import com.scms.model.OrderItem;
import com.scms.model.dto.OrderIngredientUsage;
import com.scms.repository.IngredientRepository;
import com.scms.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final IngredientRepository ingredientRepo;
    private final MenuItemRepository menuItemRepo;

    public List<Ingredient> getAll() { return ingredientRepo.findAll(); }

    public List<Ingredient> getLowStock() { return ingredientRepo.findLowStock(); }

    public Ingredient getById(Long id) {
        return ingredientRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + id));
    }

    @Transactional
    public Ingredient save(Ingredient ingredient) {
        return ingredientRepo.save(ingredient);
    }

    @Transactional
    public void restock(Long id, double amount) {
        Ingredient ing = getById(id);
        ing.restock(amount);
        ingredientRepo.save(ing);
        refreshMenuAvailability();
    }

    @Transactional
    public void reserveForOrder(Order order) {
        Map<Long, Double> required = getRequiredIngredients(order);

        for (Map.Entry<Long, Double> entry : required.entrySet()) {
            Ingredient ing = getById(entry.getKey());
            if (ing.getAvailableQuantity() < entry.getValue()) {
                throw new IllegalStateException("Insufficient stock for " + ing.getName() + ".");
            }
        }

        for (Map.Entry<Long, Double> entry : required.entrySet()) {
            Ingredient ing = getById(entry.getKey());
            ing.reserve(entry.getValue());
            ingredientRepo.save(ing);
        }
        refreshMenuAvailability();
    }

    @Transactional
    public void consumeReservedForOrder(Order order) {
        if (!Boolean.TRUE.equals(order.getInventoryReserved())) {
            throw new IllegalStateException("Inventory was not reserved for this order.");
        }
        if (Boolean.TRUE.equals(order.getInventoryConsumed())) {
            return;
        }

        Map<Long, Double> required = getRequiredIngredients(order);
        for (Map.Entry<Long, Double> entry : required.entrySet()) {
            Ingredient ing = getById(entry.getKey());
            ing.consumeReserved(entry.getValue());
            ingredientRepo.save(ing);
        }
        order.setInventoryReserved(false);
        order.setInventoryConsumed(true);
        refreshMenuAvailability();
    }

    @Transactional
    public void releaseReservationForOrder(Order order) {
        if (!Boolean.TRUE.equals(order.getInventoryReserved()) || Boolean.TRUE.equals(order.getInventoryConsumed())) {
            return;
        }

        Map<Long, Double> required = getRequiredIngredients(order);
        for (Map.Entry<Long, Double> entry : required.entrySet()) {
            Ingredient ing = getById(entry.getKey());
            ing.releaseReserved(entry.getValue());
            ingredientRepo.save(ing);
        }
        order.setInventoryReserved(false);
        refreshMenuAvailability();
    }

    @Transactional
    public boolean settleReservedForOrder(Order order, Map<Long, Double> ingredientReturnQuantities) {
        if (!Boolean.TRUE.equals(order.getInventoryReserved()) || Boolean.TRUE.equals(order.getInventoryConsumed())) {
            return false;
        }

        Map<Long, Double> required = getRequiredIngredients(order);
        Map<Long, Double> selected = ingredientReturnQuantities != null ? ingredientReturnQuantities : new HashMap<>();
        boolean anyDiscarded = false;

        for (Map.Entry<Long, Double> entry : required.entrySet()) {
            Long ingredientId = entry.getKey();
            double requiredQty = entry.getValue();
            double returnQty = selected.getOrDefault(ingredientId, 0.0);

            if (returnQty < 0) {
                throw new IllegalStateException("Returned quantity cannot be negative.");
            }
            if (returnQty > requiredQty) {
                throw new IllegalStateException("Cannot return more than reserved quantity for selected ingredient.");
            }

            Ingredient ing = getById(ingredientId);
            if (returnQty > 0) {
                ing.releaseReserved(returnQty);
            }

            double discardQty = requiredQty - returnQty;
            if (discardQty > 0) {
                anyDiscarded = true;
                ing.consumeReserved(discardQty);
            }
            ingredientRepo.save(ing);
        }

        order.setInventoryReserved(false);
        order.setInventoryConsumed(anyDiscarded);
        refreshMenuAvailability();
        return anyDiscarded;
    }

    @Transactional
    public void refreshMenuAvailability() {
        List<Ingredient> allIngredients = ingredientRepo.findAll();
        Map<Long, Double> stockMap = new java.util.HashMap<>();
        allIngredients.forEach(i -> stockMap.put(i.getId(), i.getAvailableQuantity()));

        for (MenuItem item : menuItemRepo.findAll()) {
            boolean canMake = true;
            for (Map.Entry<Long, Double> entry : item.getIngredientQuantities().entrySet()) {
                Double stock = stockMap.get(entry.getKey());
                if (stock == null || stock < entry.getValue()) { canMake = false; break; }
            }
            if (item.getAvailable() != canMake) {
                item.setAvailable(canMake);
                menuItemRepo.save(item);
            }
        }
    }

    @Transactional
    public void delete(Long id) { ingredientRepo.deleteById(id); }

    @Transactional
    public void restockSelectedIngredients(Order order, Map<Long, Double> ingredientQuantities) {
        if (ingredientQuantities == null || ingredientQuantities.isEmpty()) {
            return;
        }

        Map<Long, Double> required = getRequiredIngredients(order);
        for (Map.Entry<Long, Double> entry : ingredientQuantities.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            Double allowed = required.get(entry.getKey());
            if (allowed == null) {
                throw new IllegalStateException("Selected ingredient is not part of this order.");
            }
            if (entry.getValue() > allowed) {
                throw new IllegalStateException("Cannot return more than the ordered ingredient amount.");
            }
            Ingredient ing = getById(entry.getKey());
            ing.restock(entry.getValue());
            ingredientRepo.save(ing);
        }
        refreshMenuAvailability();
    }

    public List<OrderIngredientUsage> getIngredientUsage(Order order) {
        Map<Long, Double> required = getRequiredIngredients(order);
        List<OrderIngredientUsage> usage = new java.util.ArrayList<>();
        for (Map.Entry<Long, Double> entry : required.entrySet()) {
            usage.add(new OrderIngredientUsage(getById(entry.getKey()), entry.getValue()));
        }
        usage.sort((a, b) -> a.ingredient().getName().compareToIgnoreCase(b.ingredient().getName()));
        return usage;
    }

    public Map<Long, Double> getIngredientUsageMap(Order order) {
        return getRequiredIngredients(order);
    }

    @Transactional
    public String summarizeReturnedIngredients(Map<Long, Double> ingredientQuantities) {
        if (ingredientQuantities == null || ingredientQuantities.isEmpty()) {
            return "No ingredients were returned to inventory.";
        }

        List<String> parts = new java.util.ArrayList<>();
        for (Map.Entry<Long, Double> entry : ingredientQuantities.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            Ingredient ing = getById(entry.getKey());
            parts.add(ing.getName() + " " + String.format("%.2f", entry.getValue()) + " " + ing.getUnit());
        }
        return parts.isEmpty()
                ? "No ingredients were returned to inventory."
                : "Returned to inventory: " + String.join(", ", parts) + ".";
    }

    private Map<Long, Double> getRequiredIngredients(Order order) {
        Map<Long, Double> required = new HashMap<>();
        for (OrderItem oi : order.getItems()) {
            MenuItem mi = oi.getMenuItem();
            for (Map.Entry<Long, Double> entry : mi.getIngredientQuantities().entrySet()) {
                double qty = entry.getValue() * oi.getQuantity();
                required.merge(entry.getKey(), qty, Double::sum);
            }
        }
        return required;
    }
}
