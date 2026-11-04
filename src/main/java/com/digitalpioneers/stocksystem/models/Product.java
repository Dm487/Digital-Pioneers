package com.digitalpioneers.stocksystem.models;

import java.math.BigDecimal;

public class Product {
    public long id;
    public String sku;
    public String name;
    public String description;
    public BigDecimal unitPrice;
    public int quantity;
    public int lowStockThreshold;
    public Long supplierId;
    public String supplierName;
    public String createdAt;
    public String updatedAt;

    public Product() {}

    public Product(long id, String sku, String name, String description, BigDecimal unitPrice,
                   int quantity, int lowStockThreshold, Long supplierId, String supplierName,
                   String createdAt, String updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}