package com.borne_audi.domain;

public record Dish(
        int id,
        int categoryId,
        String name,
        String description,
        int priceCents,
        boolean available
) {}
