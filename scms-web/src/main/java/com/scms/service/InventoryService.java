package com.scms.service;

import com.scms.model.Ingredient;
import com.scms.model.MenuItem;
import com.scms.model.Order;
import com.scms.model.OrderItem;
import com.scms.repository.IngredientRepository;
import com.scms.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    public void deductForOrder(Order order) {
        for (OrderItem oi : order.getItems()) {
            MenuItem mi = oi.getMenuItem();
            for (Map.Entry<Long, Double> entry : mi.getIngredientQuantities().entrySet()) {
                Ingredient ing = getById(entry.getKey());
                ing.deduct(entry.getValue() * oi.getQuantity());
                ingredientRepo.save(ing);
            }
        }
        refreshMenuAvailability();
    }

    @Transactional
    public void refreshMenuAvailability() {
        List<Ingredient> allIngredients = ingredientRepo.findAll();
        Map<Long, Double> stockMap = new java.util.HashMap<>();
        allIngredients.forEach(i -> stockMap.put(i.getId(), i.getQuantityInStock()));

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
}
