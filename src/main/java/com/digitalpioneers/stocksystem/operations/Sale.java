package com.digitalpioneers.stocksystem.operations;

import com.digitalpioneers.stocksystem.exceptions.InsufficientStockException;
import com.digitalpioneers.stocksystem.models.SaleItem;
import com.digitalpioneers.stocksystem.services.StockManagementService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sale implements StockOperation {
    public long id;
    public BigDecimal total;
    public String createdAt;
    public List<SaleItem> items = new ArrayList<>();

    public Sale() {}

    public Sale(List<SaleItem> items) {
        this.items = items;
    }

    @Override
    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A sale must contain at least one item.");
        }
        Map<Long, Integer> requestedQuantities = new HashMap<>();
        for (SaleItem item : items) {
            if (item == null || item.productId <= 0 || item.quantity <= 0) {
                throw new IllegalArgumentException("Every sale item needs a valid productId and a quantity greater than zero.");
            }
            try {
                requestedQuantities.merge(item.productId, item.quantity, Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Combined quantity for a product is too large.");
            }
        }
    }

    @Override
    public void apply(StockManagementService service) throws InsufficientStockException {
        service.createSale(this);
    }
}