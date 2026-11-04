package com.digitalpioneers.stocksystem.services;

import com.digitalpioneers.stocksystem.models.Product;

public final class StockAlert {
    private StockAlert() {}

    public static boolean isLow(Product product) {
        return product.quantity <= product.lowStockThreshold;
    }
}