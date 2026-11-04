package com.digitalpioneers.stocksystem.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConfig implements AutoCloseable {
    private HikariDataSource dataSource;

    public DatabaseConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        initialize(Path.of(setting(dotenv, "STOCK_DB_PATH", "data/stockroom.db")));
    }

    public DatabaseConfig(Path databasePath) {
        initialize(databasePath);
    }

    private void initialize(Path databasePath) {
        Path path = databasePath.toAbsolutePath().normalize();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the SQLite database directory: " + path.getParent(), exception);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + path);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setPoolName("stockroom-sqlite-pool");
        this.dataSource = new HikariDataSource(config);
        initialize();
    }

    private static String setting(Dotenv dotenv, String key, String fallback) {
        String environmentValue = System.getenv(key);
        if (environmentValue != null && !environmentValue.isBlank()) return environmentValue;
        String fileValue = dotenv.get(key);
        return fileValue == null || fileValue.isBlank() ? fallback : fileValue;
    }

    private void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 10000");
            statement.execute("PRAGMA journal_mode = WAL");
            try (InputStream input = DatabaseConfig.class.getResourceAsStream("/db/schema.sql")) {
                if (input == null) throw new IllegalStateException("Database schema resource /db/schema.sql was not found.");
                String schema = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) statement.execute(sql);
                }
            }
            migrateLegacyProducts(connection);
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku_unique ON products (sku)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_products_name ON products (name COLLATE NOCASE)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_products_low_stock ON products (quantity, low_stock_threshold)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales (created_at DESC)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items (sale_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_stock_movements_product_created ON stock_movements (product_id, created_at DESC)");
        } catch (SQLException | IOException exception) {
            dataSource.close();
            throw new IllegalStateException("Unable to initialize the SQLite database.", exception);
        }
    }

    private static void migrateLegacyProducts(Connection connection) throws SQLException {
        if (!hasColumn(connection, "products", "sku")) {
            execute(connection, "ALTER TABLE products ADD COLUMN sku TEXT");
            execute(connection, "UPDATE products SET sku = 'LEGACY-' || id WHERE sku IS NULL OR TRIM(sku) = ''");
        }
        if (!hasColumn(connection, "products", "description")) {
            execute(connection, "ALTER TABLE products ADD COLUMN description TEXT");
        }
        if (!hasColumn(connection, "products", "unit_price")) {
            execute(connection, "ALTER TABLE products ADD COLUMN unit_price NUMERIC");
            if (hasColumn(connection, "products", "price")) {
                execute(connection, "UPDATE products SET unit_price = price WHERE unit_price IS NULL");
            }
        }
        if (!hasColumn(connection, "products", "low_stock_threshold")) {
            execute(connection, "ALTER TABLE products ADD COLUMN low_stock_threshold INTEGER NOT NULL DEFAULT 5");
            if (hasColumn(connection, "products", "min_stock_level")) {
                execute(connection, "UPDATE products SET low_stock_threshold = min_stock_level");
            }
        }
        if (!hasColumn(connection, "products", "supplier_id")) {
            execute(connection, "ALTER TABLE products ADD COLUMN supplier_id INTEGER REFERENCES suppliers(id) ON DELETE SET NULL");
        }
        if (!hasColumn(connection, "products", "created_at")) {
            execute(connection, "ALTER TABLE products ADD COLUMN created_at TEXT");
        }
        if (!hasColumn(connection, "products", "updated_at")) {
            execute(connection, "ALTER TABLE products ADD COLUMN updated_at TEXT");
        }
        execute(connection, "UPDATE products SET sku = 'LEGACY-' || id WHERE sku IS NULL OR TRIM(sku) = ''");
        execute(connection, "UPDATE products SET unit_price = 0 WHERE unit_price IS NULL");
        execute(connection, "UPDATE products SET low_stock_threshold = 5 WHERE low_stock_threshold IS NULL");
        execute(connection, "UPDATE products SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        execute(connection, "UPDATE products SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL");
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) return true;
            }
            return false;
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}