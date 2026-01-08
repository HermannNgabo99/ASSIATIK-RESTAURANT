package com.borne_audi.frontend;

import java.util.List;

public class OrderResponse {
    public String id;
    public String orderNumber;
    public String status;
    public int totalCents;
    public List<Item> items;

    public static class Item {
        public int dishId;
        public int quantity;
        public int unitPriceCents;
        public int lineTotalCents;
    }
}



