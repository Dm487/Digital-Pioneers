package com.digitalpioneers.stocksystem.controllers;

import com.digitalpioneers.stocksystem.models.Product;
import com.digitalpioneers.stocksystem.models.Supplier;
import com.digitalpioneers.stocksystem.repositories.ProductRepository;
import com.digitalpioneers.stocksystem.repositories.SupplierRepository;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import java.math.RoundingMode;

public class ProductController {
    private final ProductRepository products;
    private final SupplierRepository suppliers;

    public ProductController(ProductRepository products, SupplierRepository suppliers) {
        this.products = products;
        this.suppliers = suppliers;
    }

    public void register(Javalin app) {
        app.get("/api/products", context -> context.json(products.findAll(context.queryParam("q"))));
        app.get("/api/products/{id}", context -> {
            long id = pathId(context.pathParam("id"));
            Product product = products.findById(id)
                    .orElseThrow(() -> new NotFoundResponse("Product " + id + " was not found."));
            context.json(product);
        });
        app.post("/api/products", context -> {
            Product product = context.bodyAsClass(Product.class);
            validate(product, true);
            context.status(201).json(products.create(product));
        });
        app.put("/api/products/{id}", context -> {
            long id = pathId(context.pathParam("id"));
            Product product = context.bodyAsClass(Product.class);
            validate(product, false);
            Product updated = products.update(id, product)
                    .orElseThrow(() -> new NotFoundResponse("Product " + id + " was not found."));
            context.json(updated);
        });
        app.delete("/api/products/{id}", context -> {
            long id = pathId(context.pathParam("id"));
            if (!products.delete(id)) throw new NotFoundResponse("Product " + id + " was not found.");
            context.status(204);
        });
        app.get("/api/suppliers", context -> context.json(suppliers.findAll()));
        app.post("/api/suppliers", context -> {
            Supplier supplier = context.bodyAsClass(Supplier.class);
            validateSupplier(supplier);
            context.status(201).json(suppliers.create(supplier));
        });
        app.put("/api/suppliers/{id}", context -> {
            long id = pathId(context.pathParam("id"));
            Supplier supplier = context.bodyAsClass(Supplier.class);
            validateSupplier(supplier);
            Supplier updated = suppliers.update(id, supplier);
            if (updated == null) throw new NotFoundResponse("Supplier " + id + " was not found.");
            context.json(updated);
        });
    }

    private static void validate(Product product, boolean creating) {
        if (product == null) throw new IllegalArgumentException("Product request body is required.");
        product.sku = requiredText(product.sku, "SKU", 64);
        product.name = requiredText(product.name, "Name", 160);
        if (product.description != null && product.description.length() > 10_000) {
            throw new IllegalArgumentException("Description cannot exceed 10000 characters.");
        }
        if (product.unitPrice == null || product.unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price must be zero or greater.");
        }
        try {
            product.unitPrice = product.unitPrice.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Unit price cannot have more than two decimal places.");
        }
        if (product.unitPrice.precision() > 12) {
            throw new IllegalArgumentException("Unit price is too large.");
        }
        if (product.lowStockThreshold < 0) {
            throw new IllegalArgumentException("Low-stock threshold cannot be negative.");
        }
        if (creating && product.quantity < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative.");
        }
        if (product.supplierId != null && product.supplierId <= 0) {
            throw new IllegalArgumentException("Supplier ID must be a positive integer.");
        }
    }

    private static void validateSupplier(Supplier supplier) {
        if (supplier == null) throw new IllegalArgumentException("Supplier request body is required.");
        supplier.name = requiredText(supplier.name, "Supplier name", 120);
        checkLength(supplier.contactName, "Contact name", 120);
        checkLength(supplier.email, "Email", 254);
        checkLength(supplier.phone, "Phone", 40);
        if (supplier.email != null && !supplier.email.isBlank()
                && !supplier.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email address is invalid.");
        }
    }

    private static String requiredText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) throw new IllegalArgumentException(label + " cannot exceed " + maxLength + " characters.");
        return cleaned;
    }

    private static void checkLength(String value, String label, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(label + " cannot exceed " + maxLength + " characters.");
        }
    }

    private static long pathId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID must be a positive integer.");
        }
    }
}