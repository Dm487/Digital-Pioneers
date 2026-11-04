package com.digitalpioneers.stocksystem.models;

public class StockMovement {
    public long id;
    public long productId;
    public String productName;
    public String sku;
    public String movementType;
    public int quantityDelta;
    public String note;
    public String createdAt;

    public StockMovement(long id, long productId, String productName, String sku,
                         String movementType, int quantityDelta, String note, String createdAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.note = note;
        this.createdAt = createdAt;
    }
}
