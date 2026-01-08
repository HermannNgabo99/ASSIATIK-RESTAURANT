package com.borne_audi.dto;

import java.util.List;

public record CreateOrderRequest(
        List<CreateOrderItemDto> items
) {}

