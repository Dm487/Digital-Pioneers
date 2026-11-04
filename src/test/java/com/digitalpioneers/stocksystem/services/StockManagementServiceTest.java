package com.digitalpioneers.stocksystem.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalpioneers.stocksystem.config.DatabaseConfig;
import com.digitalpioneers.stocksystem.exceptions.InsufficientStockException;
import com.digitalpioneers.stocksystem.models.Product;
import com.digitalpioneers.stocksystem.models.SaleItem;
import com.digitalpioneers.stocksystem.models.Supplier;
import com.digitalpioneers.stocksystem.operations.Sale;
import com.digitalpioneers.stocksystem.operations.StockRestock;
import com.digitalpioneers.stocksystem.repositories.ProductRepository;
import com.digitalpioneers.stocksystem.repositories.SalesRepository;
import com.digitalpioneers.stocksystem.repositories.SupplierRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StockManagementServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsSalesRestocksAndDashboardTotalsUsingSqlite() {
        try (DatabaseConfig database = new DatabaseConfig(temporaryDirectory.resolve("workflow.db"))) {
            ProductRepository products = new ProductRepository(database);
            SalesRepository sales = new SalesRepository(database);
            SupplierRepository suppliers = new SupplierRepository(database);
            StockManagementService service = new StockManagementService(products, sales);

            Supplier supplier = new Supplier(0, "Local wholesaler", "Sam", "sam@example.com", "555-0100", null);
            supplier = suppliers.create(supplier);
            Product tea = products.create(product("TEA-1", "Breakfast tea", "4.50", 10, 9, supplier.id));
            Product coffee = products.create(product("COF-1", "Ground coffee", "7.00", 1, 0, null));

            assertEquals(2, products.dashboardStats().totalProducts);
            assertEquals(11, products.dashboardStats().totalUnitsInStock);
            assertEquals(0, products.findLowStock().size());

            Sale attemptedSale = new Sale(List.of(new SaleItem(tea.id, 2), new SaleItem(coffee.id, 2)));
            assertThrows(InsufficientStockException.class, () -> service.createSale(attemptedSale));
            assertEquals(10, products.findById(tea.id).orElseThrow().quantity,
                    "A failed multi-item sale must roll back earlier stock deductions.");
            assertTrue(sales.findRecent().isEmpty());
            assertEquals(2, products.findMovements(50).size(),
                    "The rejected sale must not add movement records.");

            Sale completed = service.createSale(new Sale(List.of(new SaleItem(tea.id, 2))));
            assertEquals(0, completed.total.compareTo(new BigDecimal("9.00")));
            assertEquals(8, products.findById(tea.id).orElseThrow().quantity);
            assertEquals(1, sales.findRecent().size());
            assertEquals(3, products.findMovements(50).size());
            assertEquals(0, products.dashboardStats().totalSales.compareTo(new BigDecimal("9.00")));
            assertEquals(1, products.dashboardStats().totalTransactions);
            assertEquals(1, products.findLowStock().size());

            StockRestock restock = new StockRestock();
            restock.productId = tea.id;
            restock.quantity = 5;
            restock.note = "Purchase order 42";
            Product restocked = service.restock(restock);
            assertEquals(13, restocked.quantity);
            assertEquals(4, products.findMovements(50).size());
            assertTrue(products.findLowStock().isEmpty());

            supplier.name = "Updated wholesaler";
            assertEquals("Updated wholesaler", suppliers.update(supplier.id, supplier).name);
            assertThrows(IllegalStateException.class, () -> products.delete(tea.id),
                    "Products with movement history must remain for audit integrity.");
        }
    }

    @Test
    void migratesLegacySqliteProductsWithoutDroppingRows() throws Exception {
        Path legacyDatabase = temporaryDirectory.resolve("legacy.db");
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + legacyDatabase);
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        price NUMERIC NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 0,
                        min_stock_level INTEGER NOT NULL DEFAULT 5,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO products (name, price, quantity, min_stock_level)
                    VALUES ('Legacy tea', 12.50, 3, 7)
                    """);
        }

        try (DatabaseConfig database = new DatabaseConfig(legacyDatabase)) {
            Product migrated = new ProductRepository(database).findById(1).orElseThrow();
            assertEquals("Legacy tea", migrated.name);
            assertEquals("LEGACY-1", migrated.sku);
            assertEquals(0, migrated.unitPrice.compareTo(new BigDecimal("12.50")));
            assertEquals(3, migrated.quantity);
            assertEquals(7, migrated.lowStockThreshold);
        }
    }

    @Test
    void lowStockIncludesThresholdAndOutOfStockProducts() {
        assertTrue(StockAlert.isLow(product(5, 5)));
        assertTrue(StockAlert.isLow(product(0, 5)));
        assertFalse(StockAlert.isLow(product(6, 5)));
    }

    private static Product product(String sku, String name, String price, int quantity, int threshold, Long supplierId) {
        Product product = new Product();
        product.sku = sku;
        product.name = name;
        product.unitPrice = new BigDecimal(price);
        product.quantity = quantity;
        product.lowStockThreshold = threshold;
        product.supplierId = supplierId;
        return product;
    }

    private static Product product(int quantity, int threshold) {
        return new Product(1, "SKU-1", "Test item", null, BigDecimal.ONE,
                quantity, threshold, null, null, null, null);
    }
}
