# Digital Pioneers Stockroom

Digital Pioneers Stockroom is a lightweight inventory and sales application built with Java 17, Javalin 5, SQLite, Maven, and a responsive vanilla HTML/CSS/JavaScript interface.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer

## Run locally

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
mvn clean verify
java -jar target/small-business-stock-system-1.0.0.jar
```

Open [http://localhost:7070](http://localhost:7070). The SQLite database is created automatically at `data/stockroom.db` on first launch, and the schema is initialized/migrated without deleting existing product records. Set `STOCK_DB_PATH` in `.env` or the process environment to use another database file. Set `PORT` to change the default port `7070`.

The application uses a single pooled SQLite connection and enables foreign-key enforcement, a write-ahead log, and a 10-second busy timeout. Keep the database file and its `-wal`/`-shm` companions together when backing up a running database; use SQLite's online backup API or stop the application first for a file copy.

## Dashboard definitions

- **Total products:** number of product records.
- **Units in stock:** sum of current product quantities.
- **Low-stock products:** number of products whose quantity is less than or equal to their reorder threshold; this includes out-of-stock products.
- **Total sales:** lifetime gross value of recorded sales, before returns or other adjustments. The application does not currently record returns.
- **Transactions:** lifetime count of recorded sales.

Dashboard figures are calculated from the database, not seeded or hardcoded. Empty tables produce zero totals and explicit empty states.

## HTTP API

Responses are JSON. Invalid request data returns `400`, missing records return `404`, stock conflicts and database constraint conflicts return `409`, and unexpected failures return `500` without exposing internal exception details.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Application and SQLite health check |
| GET | `/api/dashboard` | Real statistics, recent sales, alerts, and stock movements |
| GET | `/api/products?q=term` | List/search products |
| POST | `/api/products` | Create product |
| GET | `/api/products/{id}` | View product details |
| PUT | `/api/products/{id}` | Edit product details |
| DELETE | `/api/products/{id}` | Delete product with no sale or movement history |
| GET, POST | `/api/suppliers` | List or create suppliers |
| PUT | `/api/suppliers/{id}` | Edit supplier |
| GET | `/api/inventory` | Current product quantities |
| POST | `/api/stock/restock` | Receive stock and record a movement |
| GET | `/api/stock/movements?limit=50` | Recent stock movement history (limit 1–200) |
| GET | `/api/alerts/low-stock` | Products at or below reorder threshold |
| GET | `/api/sales` | Latest 100 transactions |
| GET | `/api/sales/{id}` | Transaction details |
| POST | `/api/sales` | Create a sale, deduct stock, and record movements atomically |

Product request example:

```json
{
  "sku": "TEA-001",
  "name": "Breakfast tea",
  "description": "Box of 100 bags",
  "unitPrice": 4.50,
  "quantity": 20,
  "lowStockThreshold": 5,
  "supplierId": null
}
```

Sale request example:

```json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

Sale prices are read from the database. Sale records, sale items, stock deductions, and stock movements commit in a single transaction. If a sale line fails validation or does not have enough stock, the transaction is rolled back.

## Data migration and deployment

The checked project did not contain a SQLite file. The database initializer can add Stockroom columns to an older SQLite `products` table that has `price`, `quantity`, and `min_stock_level`, preserving those product rows and carrying their price and reorder level forward. It does not import data from the previous PostgreSQL/Supabase configuration. Existing PostgreSQL data must be exported and deliberately migrated if it needs to be retained; the application does not delete or alter that external database.

This application does not implement authentication or authorization. It is suitable for local testing, not public production deployment. Before any public deployment, add an appropriate authentication/authorization layer, TLS, restricted network access, production logging/monitoring, and verified backups. Do not expose the database file or place it in a public static directory.
