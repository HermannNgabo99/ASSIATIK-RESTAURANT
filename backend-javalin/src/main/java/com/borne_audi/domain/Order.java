package com.borne_audi.domain;

import java.util.List;

public record Order(
        String id,
        String orderNumber,
        String status,
        int totalCents,
        String createdAt,
        List<OrderItem> items
) {}



