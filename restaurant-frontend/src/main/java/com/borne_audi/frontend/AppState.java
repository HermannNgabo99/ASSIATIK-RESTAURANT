package com.borne_audi.frontend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppState {

    // ================== PANIER ==================
    public static class CartItem {
        public int dishId;
        public String name;
        public int unitPriceCents;
        public int quantity;

        public int lineTotal() { return unitPriceCents * quantity; }
    }

    private static final Map<Integer, CartItem> CART = new LinkedHashMap<>();
    private static Map<Integer, CartItem> RECOMMENDED = null;

    public static Map<Integer, CartItem> getCart() { return CART; }

    public static void addToCart(int dishId, String name, int unitPriceCents) {
        CartItem it = CART.get(dishId);
        if (it == null) {
            it = new CartItem();
            it.dishId = dishId;
            it.name = name;
            it.unitPriceCents = unitPriceCents;
            it.quantity = 1;
            CART.put(dishId, it);
        } else {
            it.quantity++;
        }
    }

    public static void clearCart() { CART.clear(); }

    public static int cartTotalCents() {
        return CART.values().stream().mapToInt(CartItem::lineTotal).sum();
    }

    // ================== RECOMMANDÉ ==================
    public static void saveRecommendedFromCart() {
        Map<Integer, CartItem> copy = new LinkedHashMap<>();
        for (CartItem it : CART.values()) {
            CartItem c = new CartItem();
            c.dishId = it.dishId;
            c.name = it.name;
            c.unitPriceCents = it.unitPriceCents;
            c.quantity = it.quantity;
            copy.put(c.dishId, c);
        }
        RECOMMENDED = copy;
    }

    public static boolean hasRecommended() {
        return RECOMMENDED != null && !RECOMMENDED.isEmpty();
    }

    public static void loadRecommendedIntoCart() {
        if (!hasRecommended()) return;
        CART.clear();
        for (CartItem it : RECOMMENDED.values()) {
            CartItem c = new CartItem();
            c.dishId = it.dishId;
            c.name = it.name;
            c.unitPriceCents = it.unitPriceCents;
            c.quantity = it.quantity;
            CART.put(c.dishId, c);
        }
    }

    // ================== PLAT SÉLECTIONNÉ (DETAIL) ==================
    public static class DishSelection {
        public int id;
        public String name;
        public String description;
        public int priceCents;
        public boolean available;

        // ⚠️ IMPORTANT : mets un chemin classpath ABSOLU si possible:
        // ex: "/com/borne_audi/frontend/images/plats/pad_thai.png"
        public String imagePath;

        // (optionnel) si tu veux garder juste "plats/pad_thai.png"
        public String imageRel;
    }

    private static DishSelection SELECTED_DISH;

    public static void setSelectedDish(DishSelection dish) { SELECTED_DISH = dish; }
    public static DishSelection getSelectedDish() { return SELECTED_DISH; }

    // ================== LAST ORDER (confirmation) ==================
    public static class LastOrder {
        public String id;
        public String orderNumber;
        public String status;      // "CREATED" / "PAID"
        public int totalCents;
        public List<Item> items = new ArrayList<>();

        public static class Item {
            public int dishId;
            public String name;
            public int quantity;
            public int lineTotalCents;
        }
    }

    private static LastOrder LAST_ORDER;

    public static LastOrder getLastOrder() { return LAST_ORDER; }
    public static void setLastOrder(LastOrder order) { LAST_ORDER = order; }
    public static void clearLastOrder() { LAST_ORDER = null; }
}