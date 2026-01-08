package com.borne_audi.domain;

public record OrderItem(
        int dishId,
        int quantity,
        int unitPriceCents,
        int lineTotalCents
) {}

