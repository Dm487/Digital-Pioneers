package com.digitalpioneers.stocksystem.repositories;

import com.digitalpioneers.stocksystem.config.DatabaseConfig;
import com.digitalpioneers.stocksystem.models.Supplier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplierRepository {
    private final DatabaseConfig database;

    public SupplierRepository(DatabaseConfig database) {
        this.database = database;
    }

    public List<Supplier> findAll() {
        String sql = "SELECT id, name, contact_name, email, phone, created_at FROM suppliers ORDER BY name COLLATE NOCASE, id";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Supplier> suppliers = new ArrayList<>();
            while (result.next()) suppliers.add(map(result));
            return suppliers;
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public Supplier create(Supplier supplier) {
        String sql = """
                INSERT INTO suppliers (name, contact_name, email, phone) VALUES (?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplier.name.trim());
            statement.setString(2, blankToNull(supplier.contactName));
            statement.setString(3, blankToNull(supplier.email));
            statement.setString(4, blankToNull(supplier.phone));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Supplier insert did not return its generated values.");
                supplier.id = result.getLong("id");
                supplier.createdAt = result.getString("created_at");
                return supplier;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    public Supplier update(long id, Supplier supplier) {
        String sql = """
                UPDATE suppliers SET name = ?, contact_name = ?, email = ?, phone = ?
                WHERE id = ? RETURNING created_at
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplier.name.trim());
            statement.setString(2, blankToNull(supplier.contactName));
            statement.setString(3, blankToNull(supplier.email));
            statement.setString(4, blankToNull(supplier.phone));
            statement.setLong(5, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                supplier.id = id;
                supplier.createdAt = result.getString("created_at");
                return supplier;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private static Supplier map(ResultSet result) throws SQLException {
        return new Supplier(result.getLong("id"), result.getString("name"), result.getString("contact_name"),
                result.getString("email"), result.getString("phone"), result.getString("created_at"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static IllegalStateException databaseFailure(SQLException exception) {
        return new IllegalStateException("The database operation failed.", exception);
    }
}
