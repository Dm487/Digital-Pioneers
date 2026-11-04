package com.digitalpioneers.stocksystem.controllers;

import com.digitalpioneers.stocksystem.operations.Sale;
import com.digitalpioneers.stocksystem.repositories.SalesRepository;
import com.digitalpioneers.stocksystem.services.StockManagementService;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

public class SalesController {
    private final SalesRepository sales;
    private final StockManagementService stockManagement;

    public SalesController(SalesRepository sales, StockManagementService stockManagement) {
        this.sales = sales;
        this.stockManagement = stockManagement;
    }

    public void register(Javalin app) {
        app.get("/api/sales", context -> context.json(sales.findRecent()));
        app.get("/api/sales/{id}", context -> {
            long id = pathId(context.pathParam("id"));
            Sale sale = sales.findById(id);
            if (sale == null) throw new NotFoundResponse("Sale " + id + " was not found.");
            context.json(sale);
        });
        app.post("/api/sales", context -> {
            Sale sale = context.bodyAsClass(Sale.class);
            Sale completed = stockManagement.createSale(sale);
            context.status(201).json(completed);
        });
    }

    private static long pathId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID must be a positive integer.");
        }
    }
}