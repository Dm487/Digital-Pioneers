package com.digitalpioneers.stocksystem.repositories;

import com.digitalpioneers.stocksystem.config.DatabaseConfig;
import com.digitalpioneers.stocksystem.models.DashboardStats;
import com.digitalpioneers.stocksystem.models.Product;
import com.digitalpioneers.stocksystem.models.StockMovement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository {
    private static final String SELECT_COLUMNS = """
            SELECT p.id, p.sku, p.name, p.description, p.unit_price, p.quantity,
                   p.low_stock_threshold, p.supplier_id, s.name AS supplier_name,
                   p.created_at, p.updated_at
            FROM products p
            LEFT JOIN suppliers s ON s.id = p.supplier_id
            """;

    private final DatabaseConfig database;

    public ProductRepository(DatabaseConfig database) {
        this.database = database;
    }

    public List<Product> findAll(String query) {
        String sql = SELECT_COLUMNS + (query == null || query.isBlank()
                ? " ORDER BY p.name COLLATE NOCASE, p.id"
                : " WHERE LOWER(p.name) LIKE LOWER(?) OR LOWER(p.sku) LIKE LOWER(?) ORDER BY p.name COLLATE NOCASE, p.id");
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (query != null && !query.isBlank()) {
                String search = "%" + query.trim() + "%";
                statement.setString(1, search);
                statement.setString(2, search);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (result.next()) products.add(mapProduct(result));
                return products;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public List<Product> findLowStock() {
        String sql = SELECT_COLUMNS + " WHERE p.quantity <= p.low_stock_threshold ORDER BY p.quantity, p.name COLLATE NOCASE";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Product> products = new ArrayList<>();
            while (result.next()) products.add(mapProduct(result));
            return products;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public DashboardStats dashboardStats() {
        String sql = """
                SELECT COUNT(*) AS product_count,
                       COALESCE(SUM(quantity), 0) AS unit_count,
                       COALESCE(SUM(CASE WHEN quantity <= low_stock_threshold THEN 1 ELSE 0 END), 0) AS low_stock_count
                FROM products
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            long productCount = result.getLong("product_count");
            long unitCount = result.getLong("unit_count");
            long lowStockCount = result.getLong("low_stock_count");
            result.close();
            return new DashboardStats(productCount, unitCount, lowStockCount,
                    findSalesTotal(connection), findSalesCount(connection));
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public List<StockMovement> findMovements(int limit) {
        String sql = """
                SELECT m.id, m.product_id, p.name AS product_name, p.sku, m.movement_type,
                       m.quantity_delta, m.note, m.created_at
                FROM stock_movements m
                JOIN products p ON p.id = m.product_id
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT ?
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<StockMovement> movements = new ArrayList<>();
                while (result.next()) {
                    movements.add(new StockMovement(result.getLong("id"), result.getLong("product_id"),
                            result.getString("product_name"), result.getString("sku"),
                            result.getString("movement_type"), result.getInt("quantity_delta"),
                            result.getString("note"), result.getString("created_at")));
                }
                return movements;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private static java.math.BigDecimal findSalesTotal(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(total), 0) FROM sales");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getBigDecimal(1);
        }
    }

    private static long findSalesCount(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM sales");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    public Optional<Product> findById(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_COLUMNS + " WHERE p.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapProduct(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public Product create(Product product) {
        String sql = """
                INSERT INTO products (sku, name, description, unit_price, quantity, low_stock_threshold, supplier_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id, created_at, updated_at
                """;
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindProduct(statement, product, true);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Product insert did not return its generated values.");
                        product.id = result.getLong("id");
                        product.createdAt = result.getString("created_at");
                        product.updatedAt = result.getString("updated_at");
                    }
                }
                if (product.quantity > 0) {
                    try (PreparedStatement movement = connection.prepareStatement("""
                            INSERT INTO stock_movements (product_id, movement_type, quantity_delta, note)
                            VALUES (?, 'RESTOCK', ?, 'Opening stock')
                            """)) {
                        movement.setLong(1, product.id);
                        movement.setInt(2, product.quantity);
                        movement.executeUpdate();
                    }
                }
                connection.commit();
                return product;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public Optional<Product> update(long id, Product product) {
        String sql = """
                UPDATE products SET sku = ?, name = ?, description = ?, unit_price = ?,
                    low_stock_threshold = ?, supplier_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        int updated;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProduct(statement, product, false);
            statement.setLong(7, id);
            updated = statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
        return updated == 0 ? Optional.empty() : findById(id);
    }

    public boolean delete(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public void restock(long productId, int quantity, String note) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int current;
                try (PreparedStatement query = connection.prepareStatement("SELECT quantity FROM products WHERE id = ?")) {
                    query.setLong(1, productId);
                    try (ResultSet result = query.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Product " + productId + " was not found.");
                        current = result.getInt("quantity");
                    }
                }
                int updated = Math.addExact(current, quantity);
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE products SET quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    update.setInt(1, updated);
                    update.setLong(2, productId);
                    update.executeUpdate();
                }
                try (PreparedStatement movement = connection.prepareStatement(
                        "INSERT INTO stock_movements (product_id, movement_type, quantity_delta, note) VALUES (?, 'RESTOCK', ?, ?)")) {
                    movement.setLong(1, productId);
                    movement.setInt(2, quantity);
                    movement.setString(3, note);
                    movement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Restock would exceed the maximum supported stock quantity.", exception);
        }
    }

    private static void bindProduct(PreparedStatement statement, Product product, boolean includeQuantity)
            throws SQLException {
        statement.setString(1, product.sku.trim());
        statement.setString(2, product.name.trim());
        statement.setString(3, product.description == null || product.description.isBlank() ? null : product.description.trim());
        statement.setBigDecimal(4, product.unitPrice);
        if (includeQuantity) {
            statement.setInt(5, product.quantity);
            statement.setInt(6, product.lowStockThreshold);
            setSupplier(statement, 7, product.supplierId);
        } else {
            statement.setInt(5, product.lowStockThreshold);
            setSupplier(statement, 6, product.supplierId);
        }
    }

    private static void setSupplier(PreparedStatement statement, int index, Long supplierId) throws SQLException {
        if (supplierId == null) statement.setNull(index, java.sql.Types.BIGINT);
        else statement.setLong(index, supplierId);
    }

    private static Product mapProduct(ResultSet result) throws SQLException {
        long supplierId = result.getLong("supplier_id");
        Long nullableSupplierId = result.wasNull() ? null : supplierId;
        return new Product(result.getLong("id"), result.getString("sku"), result.getString("name"),
                result.getString("description"), result.getBigDecimal("unit_price"), result.getInt("quantity"),
                result.getInt("low_stock_threshold"), nullableSupplierId, result.getString("supplier_name"),
                result.getString("created_at"), result.getString("updated_at"));
    }

    private static IllegalStateException databaseFailure(SQLException exception) {
        return new IllegalStateException("The database operation failed.", exception);
    }
}
