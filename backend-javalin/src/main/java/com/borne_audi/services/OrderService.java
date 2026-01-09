package com.borne_audi.services;

import com.borne_audi.domain.Order;
import com.borne_audi.domain.OrderItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderService {

    // Simulation de base de données en mémoire
    private static final List<Order> ordersDatabase = new ArrayList<>();

    public Order createOrder(List<OrderItem> items) {
        // 1. Calcul du total
        int total = 0;
        for (OrderItem item : items) {
            // CORRECTION : On utilise le vrai nom que j'ai vu sur ta capture
            total += item.unitPriceCents() * item.quantity();
        }

        // 2. Préparation des infos
        String orderId = UUID.randomUUID().toString();
        String orderNumber = "ORD-" + System.currentTimeMillis();
        String createdAt = Instant.now().toString();
        String status = "PENDING"; // CORRECTION : Il manquait le status !

        // 3. Création de l'objet Order
        // CORRECTION : L'ordre exact est : id, orderNumber, status, totalCents, createdAt, items
        Order newOrder = new Order(orderId, orderNumber, status, total, createdAt, items);

        // 4. Sauvegarde
        ordersDatabase.add(newOrder);

        return newOrder;
    }

    public List<Order> getAllOrders() {
        return ordersDatabase;
    }
}