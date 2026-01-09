package com.borne_audi.controllers;

import com.borne_audi.domain.OrderItem;
import com.borne_audi.dto.CreateOrderItemDto;
import com.borne_audi.dto.CreateOrderRequest;
import com.borne_audi.services.OrderService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

public class OrderController {

    private static final OrderService service = new OrderService();

    public static void create(Context ctx) {
        // 1. On récupère la demande brute (DTO)
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);

        // 2. CONVERSION : On transforme la demande (DTO) en vrais objets (OrderItem)
        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderItemDto dto : request.items()) {
            // NOTE : Normalement, on cherche le prix du plat en DB ici.
            // Pour l'instant, on met un prix par défaut (10.00€) pour valider l'architecture.
            int unitPrice = 1000;
            int lineTotal = unitPrice * dto.quantity();

            items.add(new OrderItem(
                    dto.dishId(),
                    dto.quantity(),
                    unitPrice,
                    lineTotal
            ));
        }

        // 3. On envoie la liste propre au service
        var newOrder = service.createOrder(items);

        // 4. On répond
        ctx.status(201).json(newOrder);
    }

    public static void getAll(Context ctx) {
        ctx.json(service.getAllOrders());
    }
}