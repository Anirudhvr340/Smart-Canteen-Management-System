package com.scms.config;

import com.scms.model.*;
import com.scms.model.enums.Role;
import com.scms.repository.*;
import com.scms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepo;
    private final IngredientRepository ingredientRepo;
    private final MenuItemRepository menuItemRepo;
    private final CouponRepository couponRepo;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) { log.info("DB already seeded — skipping."); return; }
        log.info("Seeding initial data...");

        // ── Users ────────────────────────────────────────────────────────────
        userService.register("Admin",          "admin@scms.com",   "admin123",  Role.ADMIN);
        userService.register("Chef Ravi",      "staff@scms.com",   "staff123",  Role.STAFF);
        userService.register("Stock Manager",  "stock@scms.com",   "stock123",  Role.INVENTORY_MANAGER);
        User customer = userService.register("Arjun Kumar", "arjun@scms.com", "pass123", Role.CUSTOMER);
        customer.setWalletBalance(1000.0);
        userService.save(customer);

        // ── Ingredients ──────────────────────────────────────────────────────
        Ingredient flour   = save(ing("Flour",        2000, 300, "g",  0.05));
        Ingredient cheese  = save(ing("Cheese",       800,  100, "g",  0.50));
        Ingredient tomato  = save(ing("Tomato Sauce", 1500, 200, "ml", 0.08));
        Ingredient milk    = save(ing("Milk",         3000, 500, "ml", 0.04));
        Ingredient chicken = save(ing("Chicken",      2000, 300, "g",  0.60));
        Ingredient rice    = save(ing("Basmati Rice", 5000, 500, "g",  0.03));
        Ingredient oil     = save(ing("Cooking Oil",  2000, 200, "ml", 0.06));

        // ── Menu items ───────────────────────────────────────────────────────
        MenuItem pizza = menuItemRepo.save(MenuItem.builder()
                .name("Margherita Pizza").description("Classic tomato & mozzarella")
                .category("Main Course").price(220.0).prepTimeMinutes(15)
                .caloriesPerServing(680).dietaryTags("VEG").build());
        pizza.getIngredientQuantities().put(flour.getId(), 150.0);
        pizza.getIngredientQuantities().put(cheese.getId(), 80.0);
        pizza.getIngredientQuantities().put(tomato.getId(), 60.0);
        pizza.getIngredientQuantities().put(oil.getId(), 10.0);
        menuItemRepo.save(pizza);

        MenuItem burger = menuItemRepo.save(MenuItem.builder()
                .name("Veg Burger").description("Crispy patty with fresh veggies")
                .category("Snacks").price(120.0).prepTimeMinutes(8)
                .caloriesPerServing(420).dietaryTags("VEG").build());
        burger.getIngredientQuantities().put(flour.getId(), 80.0);
        burger.getIngredientQuantities().put(cheese.getId(), 30.0);
        burger.getIngredientQuantities().put(oil.getId(), 15.0);
        menuItemRepo.save(burger);

        MenuItem paneer = menuItemRepo.save(MenuItem.builder()
                .name("Paneer Tikka").description("Grilled paneer with spices")
                .category("Starters").price(180.0).prepTimeMinutes(10)
                .caloriesPerServing(390).dietaryTags("VEG,GLUTEN_FREE").build());
        paneer.getIngredientQuantities().put(cheese.getId(), 100.0);
        paneer.getIngredientQuantities().put(oil.getId(), 20.0);
        menuItemRepo.save(paneer);

        MenuItem biryani = menuItemRepo.save(MenuItem.builder()
                .name("Chicken Biryani").description("Aromatic basmati with spiced chicken")
                .category("Main Course").price(280.0).prepTimeMinutes(20)
                .caloriesPerServing(750).build());
        biryani.getIngredientQuantities().put(rice.getId(), 250.0);
        biryani.getIngredientQuantities().put(chicken.getId(), 200.0);
        biryani.getIngredientQuantities().put(oil.getId(), 25.0);
        menuItemRepo.save(biryani);

        menuItemRepo.save(MenuItem.builder()
                .name("Masala Chai").description("Spiced Indian tea with milk")
                .category("Beverages").price(30.0).prepTimeMinutes(5)
                .caloriesPerServing(90).dietaryTags("VEG").build());

        menuItemRepo.save(MenuItem.builder()
                .name("Gulab Jamun").description("Soft milk-solid dumplings in sugar syrup")
                .category("Desserts").price(60.0).prepTimeMinutes(3)
                .caloriesPerServing(310).dietaryTags("VEG").build());

        // ── Coupons ──────────────────────────────────────────────────────────
        couponRepo.save(Coupon.builder()
                .code("WELCOME50").type(Coupon.Type.FLAT_AMOUNT).value(50.0)
                .minOrderValue(100.0).maxDiscount(50.0).usageLimit(100)
                .expiresAt(LocalDateTime.now().plusDays(30)).build());

        couponRepo.save(Coupon.builder()
                .code("FEST20").type(Coupon.Type.PERCENTAGE).value(20.0)
                .minOrderValue(200.0).maxDiscount(150.0).usageLimit(50)
                .expiresAt(LocalDateTime.now().plusDays(7)).build());

        log.info("Seeding complete.");
    }

    private Ingredient save(Ingredient i) { return ingredientRepo.save(i); }

    private Ingredient ing(String name, double qty, double threshold, String unit, double cost) {
        return Ingredient.builder().name(name).quantityInStock(qty)
                .lowStockThreshold(threshold).unit(unit).costPerUnit(cost).build();
    }
}
