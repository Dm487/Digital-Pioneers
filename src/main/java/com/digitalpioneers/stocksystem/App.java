package com.digitalpioneers.stocksystem;

import com.digitalpioneers.stocksystem.config.DatabaseConfig;
import com.digitalpioneers.stocksystem.controllers.ExceptionHandlers;
import com.digitalpioneers.stocksystem.controllers.DashboardController;
import com.digitalpioneers.stocksystem.controllers.ProductController;
import com.digitalpioneers.stocksystem.controllers.SalesController;
import com.digitalpioneers.stocksystem.controllers.StockController;
import com.digitalpioneers.stocksystem.repositories.ProductRepository;
import com.digitalpioneers.stocksystem.repositories.SalesRepository;
import com.digitalpioneers.stocksystem.repositories.SupplierRepository;
import com.digitalpioneers.stocksystem.services.Inventory;
import com.digitalpioneers.stocksystem.services.StockManagementService;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.Map;

public final class App {
    private App() {}

    public static void main(String[] args) {
        DatabaseConfig database = new DatabaseConfig();
        ProductRepository products = new ProductRepository(database);
        SalesRepository sales = new SalesRepository(database);
        SupplierRepository suppliers = new SupplierRepository(database);
        Inventory inventory = new Inventory(products);
        StockManagementService stockService = new StockManagementService(products, sales);

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String configuredPort = System.getenv().getOrDefault("PORT", dotenv.get("PORT", "7070"));
        int port = Integer.parseInt(configuredPort);
        if (port < 1 || port > 65_535) {
            throw new IllegalStateException("PORT must be between 1 and 65535.");
        }
        Javalin app = Javalin.create(config -> config.staticFiles.add(staticFiles -> {
            staticFiles.hostedPath = "/";
            staticFiles.directory = "/public";
            staticFiles.location = Location.CLASSPATH;
        }));
        ExceptionHandlers.register(app);
        new DashboardController(products, sales).register(app);
        new ProductController(products, suppliers).register(app);
        new SalesController(sales, stockService).register(app);
        new StockController(inventory, stockService).register(app);
        app.get("/", ctx -> ctx.redirect("/index.html"));
        app.get("/api/health", ctx -> {
            try (var connection = database.getConnection()) {
                if (!connection.isValid(2)) throw new IllegalStateException("SQLite connection is not valid.");
                ctx.json(Map.of("status", "ok", "database", "connected"));
            }
        });
        app.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }
}
