package com.digitalpioneers.stocksystem.models;

import java.math.BigDecimal;

public class SaleItem {
    public long productId;
    public String productName;
    public int quantity;
    public BigDecimal unitPrice;
    public BigDecimal lineTotal;

    public SaleItem() {}

    public SaleItem(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public SaleItem(long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }
}