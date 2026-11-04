package com.digitalpioneers.stocksystem.operations;

import com.digitalpioneers.stocksystem.services.StockManagementService;

public class StockRestock implements StockOperation {
    public long productId;
    public int quantity;
    public String note;

    public StockRestock() {}

    @Override
    public void validate() {
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be a positive integer.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be greater than zero.");
        }
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException("Restock note cannot exceed 500 characters.");
        }
    }

    @Override
    public void apply(StockManagementService service) {
        service.restock(this);
    }
}