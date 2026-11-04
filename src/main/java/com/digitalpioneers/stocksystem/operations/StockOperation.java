package com.digitalpioneers.stocksystem.operations;

import com.digitalpioneers.stocksystem.exceptions.InsufficientStockException;
import com.digitalpioneers.stocksystem.services.StockManagementService;

public interface StockOperation {
    void validate();
    void apply(StockManagementService service) throws InsufficientStockException;
}