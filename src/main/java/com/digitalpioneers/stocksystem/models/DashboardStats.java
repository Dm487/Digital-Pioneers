package com.digitalpioneers.stocksystem.models;

import java.math.BigDecimal;

public class DashboardStats {
    public long totalProducts;
    public long totalUnitsInStock;
    public long lowStockProducts;
    public BigDecimal totalSales;
    public long totalTransactions;

    public DashboardStats(long totalProducts, long totalUnitsInStock, long lowStockProducts,
                          BigDecimal totalSales, long totalTransactions) {
        this.totalProducts = totalProducts;
        this.totalUnitsInStock = totalUnitsInStock;
        this.lowStockProducts = lowStockProducts;
        this.totalSales = totalSales;
        this.totalTransactions = totalTransactions;
    }
}
