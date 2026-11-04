package com.digitalpioneers.stocksystem.repositories;

import com.digitalpioneers.stocksystem.config.DatabaseConfig;
import com.digitalpioneers.stocksystem.exceptions.InsufficientStockException;
import com.digitalpioneers.stocksystem.models.SaleItem;
import com.digitalpioneers.stocksystem.operations.Sale;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SalesRepository {
    private final DatabaseConfig database;

    public SalesRepository(DatabaseConfig database) {
        this.database = database;
    }

    public Sale createSale(Sale requested) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Sale sale = new Sale();
                sale.items = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                Map<Long, Integer> quantities = new TreeMap<>();
                for (SaleItem item : requested.items) {
                    quantities.merge(item.productId, item.quantity, Math::addExact);
                }
                try (PreparedStatement productQuery = connection.prepareStatement(
                        "SELECT name, unit_price, quantity FROM products WHERE id = ?");
                     PreparedStatement stockUpdate = connection.prepareStatement(
                             "UPDATE products SET quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?");
                     PreparedStatement movementInsert = connection.prepareStatement(
                             "INSERT INTO stock_movements (product_id, movement_type, quantity_delta, note) VALUES (?, 'SALE', ?, ?)")) {
                    for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                        long productId = entry.getKey();
                        int quantity = entry.getValue();
                        productQuery.setLong(1, productId);
                        String name;
                        BigDecimal price;
                        int available;
                        try (ResultSet product = productQuery.executeQuery()) {
                            if (!product.next()) {
                                throw new IllegalArgumentException("Product " + productId + " was not found.");
                            }
                            name = product.getString("name");
                            price = product.getBigDecimal("unit_price");
                            available = product.getInt("quantity");
                        }
                        if (available < quantity) {
                            throw new InsufficientStockException("Not enough stock for " + name
                                    + ". Available: " + available + ", requested: " + quantity + ".");
                        }
                        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                        total = total.add(lineTotal);
                        sale.items.add(new SaleItem(productId, name, quantity, price, lineTotal));

                        stockUpdate.setInt(1, available - quantity);
                        stockUpdate.setLong(2, productId);
                        stockUpdate.executeUpdate();
                        movementInsert.setLong(1, productId);
                        movementInsert.setInt(2, -quantity);
                        movementInsert.setString(3, "Sale");
                        movementInsert.executeUpdate();
                    }
                }

                try (PreparedStatement insertSale = connection.prepareStatement(
                        "INSERT INTO sales (total) VALUES (?) RETURNING id, total, created_at")) {
                    insertSale.setBigDecimal(1, total);
                    try (ResultSet result = insertSale.executeQuery()) {
                        result.next();
                        sale.id = result.getLong("id");
                        sale.total = result.getBigDecimal("total");
                        sale.createdAt = result.getString("created_at");
                    }
                }
                try (PreparedStatement insertItem = connection.prepareStatement("""
                        INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, line_total)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                    for (SaleItem item : sale.items) {
                        insertItem.setLong(1, sale.id);
                        insertItem.setLong(2, item.productId);
                        insertItem.setString(3, item.productName);
                        insertItem.setInt(4, item.quantity);
                        insertItem.setBigDecimal(5, item.unitPrice);
                        insertItem.setBigDecimal(6, item.lineTotal);
                        insertItem.addBatch();
                    }
                    insertItem.executeBatch();
                }
                connection.commit();
                return sale;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("The database operation failed.", exception);
        }
    }

    public List<Sale> findRecent() {
        return findRecent(100);
    }

    public List<Sale> findRecent(int limit) {
        String sql = "SELECT id, total, created_at FROM sales ORDER BY created_at DESC, id DESC LIMIT ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<Sale> sales = new ArrayList<>();
                while (result.next()) {
                    Sale sale = new Sale();
                    sale.id = result.getLong("id");
                    sale.total = result.getBigDecimal("total");
                    sale.createdAt = result.getString("created_at");
                    sale.items = findItems(connection, sale.id);
                    sales.add(sale);
                }
                return sales;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("The database operation failed.", exception);
        }
    }

    public Sale findById(long id) {
        String sql = "SELECT id, total, created_at FROM sales WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                Sale sale = new Sale();
                sale.id = result.getLong("id");
                sale.total = result.getBigDecimal("total");
                sale.createdAt = result.getString("created_at");
                sale.items = findItems(connection, sale.id);
                return sale;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("The database operation failed.", exception);
        }
    }

    private static List<SaleItem> findItems(Connection connection, long saleId) throws SQLException {
        String sql = """
                SELECT product_id, product_name, quantity, unit_price, line_total
                FROM sale_items WHERE sale_id = ? ORDER BY id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet result = statement.executeQuery()) {
                List<SaleItem> items = new ArrayList<>();
                while (result.next()) {
                    items.add(new SaleItem(result.getLong("product_id"), result.getString("product_name"),
                            result.getInt("quantity"), result.getBigDecimal("unit_price"),
                            result.getBigDecimal("line_total")));
                }
                return items;
            }
        }
    }
}