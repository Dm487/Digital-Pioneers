package com.digitalpioneers.stocksystem.services;

import com.digitalpioneers.stocksystem.models.Product;
import com.digitalpioneers.stocksystem.operations.Sale;
import com.digitalpioneers.stocksystem.operations.StockRestock;
import com.digitalpioneers.stocksystem.repositories.ProductRepository;
import com.digitalpioneers.stocksystem.repositories.SalesRepository;
import java.util.Optional;

public class StockManagementService {
    private final ProductRepository products;
    private final SalesRepository sales;

    public StockManagementService(ProductRepository products, SalesRepository sales) {
        this.products = products;
        this.sales = sales;
    }

    public Sale createSale(Sale sale) {
        if (sale == null) throw new IllegalArgumentException("Sale request body is required.");
        sale.validate();
        return sales.createSale(sale);
    }

    public Product restock(StockRestock restock) {
        if (restock == null) throw new IllegalArgumentException("Restock request body is required.");
        restock.validate();
        products.restock(restock.productId, restock.quantity, restock.note);
        return products.findById(restock.productId)
                .orElseThrow(() -> new IllegalArgumentException("Product " + restock.productId + " was not found."));
    }

    public Optional<Product> findProduct(long id) {
        return products.findById(id);
    }
}