package com.borne_audi;

import com.borne_audi.domain.Category;
import com.borne_audi.domain.Dish;
import com.borne_audi.domain.Order;
import com.borne_audi.domain.OrderItem;
import com.borne_audi.dto.CreateOrderItemDto;
import com.borne_audi.dto.CreateOrderRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class App {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path ORDERS_FILE = DATA_DIR.resolve("orders.json");

    public static void main(String[] args) {

        int port = 9090;

        // Jackson (déjà dans ton projet via dependencies)
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        // ===== Seed data =====
        List<Category> categories = List.of(
                new Category(1, "Entrees"),
                new Category(2, "Plats"),
                new Category(3, "Boissons / Desserts")
        );

        List<Dish> dishes = List.of(
                new Dish(101, 1, "Gyozas", "Raviolis japonais", 650, true),
                new Dish(201, 2, "Pad Thai", "Nouilles sautées", 1290, true),
                new Dish(202, 2, "Ramen", "Bouillon + nouilles", 1390, false),
                new Dish(301, 3, "Bubble Tea", "Thé perlé", 590, true)
        );

        // ===== In-memory store + load from disk =====
        Map<String, Order> orders = new HashMap<>();
        loadOrdersFromDisk(mapper, orders);

        // next order number based on file
        final int[] seq = {computeNextSequence(orders)};

        Javalin app = Javalin.create(cfg -> {
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        app.get("/", ctx -> ctx.result("API OK. Try /health, /categories, /dishes, /orders, POST /orders"));

        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        app.get("/categories", ctx -> ctx.json(categories));

        app.get("/dishes", ctx -> {
            String cat = ctx.queryParam("categoryId");
            if (cat == null) {
                ctx.json(dishes);
                return;
            }
            int categoryId;
            try {
                categoryId = Integer.parseInt(cat);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "categoryId must be an integer"));
                return;
            }
            ctx.json(dishes.stream().filter(d -> d.categoryId() == categoryId).toList());
        });

        // GET /orders (list all)
        app.get("/orders", ctx -> ctx.json(orders.values()));

        // POST /orders (create)
        app.post("/orders", ctx -> {
            CreateOrderRequest req;
            try {
                req = ctx.bodyAsClass(CreateOrderRequest.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Invalid JSON body", "details", e.getMessage()));
                return;
            }

            if (req == null || req.items() == null || req.items().isEmpty()) {
                ctx.status(400).json(Map.of("error", "Order must contain items"));
                return;
            }

            int total = 0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CreateOrderItemDto item : req.items()) {
                if (item.quantity() <= 0) {
                    ctx.status(400).json(Map.of("error", "Quantity must be > 0"));
                    return;
                }

                Dish dish = dishes.stream()
                        .filter(d -> d.id() == item.dishId())
                        .findFirst()
                        .orElse(null);

                if (dish == null) {
                    ctx.status(400).json(Map.of("error", "Dish not found: " + item.dishId()));
                    return;
                }

                if (!dish.available()) {
                    ctx.status(400).json(Map.of("error", "Dish unavailable: " + item.dishId()));
                    return;
                }

                int lineTotal = dish.priceCents() * item.quantity();
                total += lineTotal;

                orderItems.add(new OrderItem(
                        dish.id(),
                        item.quantity(),
                        dish.priceCents(),
                        lineTotal
                ));
            }

            String orderId = UUID.randomUUID().toString();
            String orderNumber = "ORD-" + String.format("%04d", seq[0]++);
            String createdAt = Instant.now().toString(); // ISO lisible

            Order order = new Order(
                    orderId,
                    orderNumber,
                    "CREATED",
                    total,
                    createdAt,
                    orderItems
            );

            orders.put(order.id(), order);
            saveOrdersToDisk(mapper, orders);

            ctx.status(201).json(order);
        });

        // GET /orders/{id}
        app.get("/orders/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Order order = orders.get(id);
            if (order == null) {
                ctx.status(404).json(Map.of("error", "Order not found"));
                return;
            }
            ctx.json(order);
        });

        // PUT /orders/{id}/status  body: {"status":"PAID"} or {"status":"CANCELLED"}
        app.put("/orders/{id}/status", ctx -> {
            String id = ctx.pathParam("id");

            Order existing = orders.get(id);
            if (existing == null) {
                ctx.status(404).json(Map.of("error", "Order not found"));
                return;
            }

            Map<?, ?> body;
            try {
                body = ctx.bodyAsClass(Map.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Invalid JSON body"));
                return;
            }

            Object statusObj = body.get("status");
            if (statusObj == null) {
                ctx.status(400).json(Map.of("error", "Missing field: status"));
                return;
            }

            String newStatus = statusObj.toString().trim().toUpperCase();
            Set<String> allowed = Set.of("CREATED", "PAID", "CANCELLED");
            if (!allowed.contains(newStatus)) {
                ctx.status(400).json(Map.of("error", "Invalid status. Allowed: " + allowed));
                return;
            }

            Order updated = new Order(
                    existing.id(),
                    existing.orderNumber(),
                    newStatus,
                    existing.totalCents(),
                    existing.createdAt(),
                    existing.items()
            );

            orders.put(updated.id(), updated);
            saveOrdersToDisk(mapper, orders);

            ctx.json(updated);
        });

        app.start(port);
        System.out.println("Server started: http://localhost:" + port);
        System.out.println("Orders file: " + ORDERS_FILE.toAbsolutePath());
    }

    private static void loadOrdersFromDisk(ObjectMapper mapper, Map<String, Order> orders) {
        try {
            if (!Files.exists(ORDERS_FILE)) return;

            List<Order> list = mapper.readValue(ORDERS_FILE.toFile(), new TypeReference<>() {});
            for (Order o : list) {
                orders.put(o.id(), o);
            }
            System.out.println("Loaded " + list.size() + " orders from " + ORDERS_FILE);
        } catch (Exception e) {
            System.out.println("WARNING: failed to load orders.json: " + e.getMessage());
        }
    }

    private static void saveOrdersToDisk(ObjectMapper mapper, Map<String, Order> orders) {
        try {
            Files.createDirectories(DATA_DIR);
            List<Order> list = new ArrayList<>(orders.values());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(ORDERS_FILE.toString()), list);
        } catch (Exception e) {
            System.out.println("WARNING: failed to save orders.json: " + e.getMessage());
        }
    }

    private static int computeNextSequence(Map<String, Order> orders) {
        int max = 0;
        for (Order o : orders.values()) {
            String num = o.orderNumber(); // "ORD-0001"
            if (num != null && num.startsWith("ORD-")) {
                try {
                    int n = Integer.parseInt(num.substring(4));
                    if (n > max) max = n;
                } catch (Exception ignored) {}
            }
        }
        return max + 1;
    }
}
