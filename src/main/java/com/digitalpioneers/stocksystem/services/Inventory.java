package com.digitalpioneers.stocksystem.services;

import com.digitalpioneers.stocksystem.models.Product;
import com.digitalpioneers.stocksystem.models.StockMovement;
import com.digitalpioneers.stocksystem.repositories.ProductRepository;
import java.util.List;

public class Inventory {
    private final ProductRepository products;

    public Inventory(ProductRepository products) {
        this.products = products;
    }

    public List<Product> list() {
        return products.findAll(null);
    }

    public List<Product> lowStock() {
        return products.findLowStock();
    }

    public List<StockMovement> movements(int limit) {
        return products.findMovements(limit);
    }
}