package com.scms.service;

import com.scms.model.MenuItem;
import com.scms.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuRepo;

    public List<MenuItem> getAll() { return menuRepo.findAllByOrderByCategory(); }

    public List<MenuItem> getAvailable() { return menuRepo.findByAvailableTrue(); }

    public MenuItem getById(Long id) {
        return menuRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + id));
    }

    public List<MenuItem> searchByName(String keyword) {
        return menuRepo.findByNameContainingIgnoreCaseAndAvailableTrue(keyword);
    }

    public List<MenuItem> getByCategory(String category) {
        return menuRepo.findByCategory(category);
    }

    @Transactional
    public MenuItem save(MenuItem item) {
        return menuRepo.save(item);
    }

    @Transactional
    public void toggleAvailability(Long id) {
        MenuItem item = getById(id);
        item.setAvailable(!item.getAvailable());
        menuRepo.save(item);
    }

    @Transactional
    public void delete(Long id) { menuRepo.deleteById(id); }

    @Transactional
    public void mapIngredient(Long itemId, Long ingredientId, Double qty) {
        MenuItem item = getById(itemId);
        item.getIngredientQuantities().put(ingredientId, qty);
        menuRepo.save(item);
    }
}
