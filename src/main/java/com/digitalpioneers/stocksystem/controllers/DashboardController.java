package com.digitalpioneers.stocksystem.controllers;

import com.digitalpioneers.stocksystem.repositories.ProductRepository;
import com.digitalpioneers.stocksystem.repositories.SalesRepository;
import io.javalin.Javalin;
import java.util.Map;

public class DashboardController {
    private final ProductRepository products;
    private final SalesRepository sales;

    public DashboardController(ProductRepository products, SalesRepository sales) {
        this.products = products;
        this.sales = sales;
    }

    public void register(Javalin app) {
        app.get("/api/dashboard", context -> context.json(Map.of(
                "stats", products.dashboardStats(),
                "recentSales", sales.findRecent(8),
                "lowStockProducts", products.findLowStock().stream().limit(8).toList(),
                "recentMovements", products.findMovements(8)
        )));
    }
}
