package com.digitalpioneers.stocksystem.controllers;

import com.digitalpioneers.stocksystem.operations.StockRestock;
import com.digitalpioneers.stocksystem.services.Inventory;
import com.digitalpioneers.stocksystem.services.StockManagementService;
import io.javalin.Javalin;

public class StockController {
    private final Inventory inventory;
    private final StockManagementService stockManagement;

    public StockController(Inventory inventory, StockManagementService stockManagement) {
        this.inventory = inventory;
        this.stockManagement = stockManagement;
    }

    public void register(Javalin app) {
        app.get("/api/inventory", context -> context.json(inventory.list()));
        app.get("/api/alerts/low-stock", context -> context.json(inventory.lowStock()));
        app.get("/api/stock/movements", context -> {
            int limit = 50;
            String requestedLimit = context.queryParam("limit");
            if (requestedLimit != null) {
                try {
                    limit = Integer.parseInt(requestedLimit);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("limit must be an integer between 1 and 200.");
                }
            }
            if (limit < 1 || limit > 200) {
                throw new IllegalArgumentException("limit must be an integer between 1 and 200.");
            }
            context.json(inventory.movements(limit));
        });
        app.post("/api/stock/restock", context -> {
            StockRestock restock = context.bodyAsClass(StockRestock.class);
            context.json(stockManagement.restock(restock));
        });
    }
}