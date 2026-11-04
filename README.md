# Digital Pioneers Stockroom

**A lightweight Inventory and Sales Management System for Small Businesses.**

Digital Pioneers Stockroom is an inventory and sales management application designed to help small businesses manage products, monitor stock levels, record sales, and track inventory movements efficiently.

Built with **Java 17, Javalin 5, SQLite, Maven, and Vanilla HTML, CSS, and JavaScript**, the application provides a responsive interface and a lightweight database solution suitable for affordable computers and devices.

---

## Project Team

**Group Name:** Digital Pioneers

| No. | Student Name | Student ID |
|:---:|---|:---:|
| 1 | Moses Phiri | 2410103 |
| 2 | Davies Mubambe | 2410052 |
| 3 | David Chisenga | 2410348 |
| 4 | [Fourth Member] | [Student ID] |

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core application logic |
| Javalin 5 | Backend web framework and REST API |
| SQLite | Lightweight database |
| Maven | Dependency and build management |
| HTML5 | Frontend structure |
| CSS3 | Styling and responsive layouts |
| JavaScript | Frontend functionality and API communication |

---

## Key Features

- **Product Management:** Register, view, search, update, and delete products.
- **Inventory Tracking:** Monitor available stock quantities.
- **Sales Management:** Record sales and automatically deduct stock.
- **Supplier Management:** Maintain supplier information.
- **Low-Stock Alerts:** Identify products that have reached or fallen below their reorder thresholds.
- **Dashboard:** View inventory statistics, sales figures, recent transactions, and stock alerts.
- **Stock Movement History:** Track stock additions and deductions.
- **Database Persistence:** Store application records in SQLite.

---

## Requirements

Before running the application, ensure you have installed:

- Java Development Kit (JDK) 17 or newer.
- Apache Maven 3.8 or newer.

Verify your installations:

```bash
java -version
mvn -version
```

---

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
```

Navigate into the project directory:

```bash
cd Digital-Pioneers
```

### 2. Configure Environment Variables

In PowerShell, create your local environment configuration:

```powershell
if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
}
```

### 3. Build the Application

Run:

```bash
mvn clean verify
```

This cleans the previous build, compiles the application, runs verification, and packages the application.

### 4. Start the Application

```bash
java -jar target/small-business-stock-system-1.0.0.jar
```

### 5. Open the Application

Visit:

**http://localhost:7070**

The SQLite database is automatically created at:

```text
data/stockroom.db
```

The database schema is initialized and migrated automatically on first launch.

---

## Configuration

The application supports the following environment variables:

| Variable | Default | Description |
|---|---|---|
| `PORT` | `7070` | Application server port |
| `STOCK_DB_PATH` | `data/stockroom.db` | SQLite database file location |

You can modify these values in `.env` or through your system's process environment.

### Database Configuration

The application uses SQLite with:

- Foreign-key enforcement.
- Write-ahead logging (WAL).
- A 10-second busy timeout.
- A single pooled SQLite connection.

**Important:** When backing up a running database, keep the database file and its associated `-wal` and `-shm` files together. Alternatively, stop the application before copying the database file or use SQLite's online backup API.

---

## Dashboard Statistics

The dashboard calculates its statistics directly from the database.

| Statistic | Definition |
|---|---|
| Total Products | Number of registered product records. |
| Units in Stock | Sum of the current quantities of all products. |
| Low-Stock Products | Products whose quantities are less than or equal to their reorder thresholds, including out-of-stock products. |
| Total Sales | Lifetime gross value of recorded sales before returns or adjustments. |
| Transactions | Lifetime number of recorded sales. |

Empty database tables produce zero totals and explicit empty states rather than hardcoded sample statistics.

**Note:** The application does not currently record sales returns.

---

## HTTP REST API

The backend exposes a REST API that communicates using JSON.

### API Endpoints

#### System

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Application and SQLite health check |
| GET | `/api/dashboard` | Dashboard statistics, recent sales, alerts, and stock movements |

#### Products

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products?q=term` | List and search products |
| POST | `/api/products` | Create a product |
| GET | `/api/products/{id}` | View product details |
| PUT | `/api/products/{id}` | Update product details |
| DELETE | `/api/products/{id}` | Delete a product without sale or movement history |

#### Suppliers

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/suppliers` | List suppliers |
| POST | `/api/suppliers` | Create a supplier |
| PUT | `/api/suppliers/{id}` | Update supplier details |

#### Inventory and Stock

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/inventory` | View current product quantities |
| POST | `/api/stock/restock` | Receive stock and record a movement |
| GET | `/api/stock/movements?limit=50` | View recent stock movement history |
| GET | `/api/alerts/low-stock` | Retrieve low-stock products |

The stock movement history endpoint supports limits from 1 to 200 records.

#### Sales

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/sales` | Retrieve the latest 100 transactions |
| GET | `/api/sales/{id}` | View transaction details |
| POST | `/api/sales` | Record a sale, deduct stock, and record movements atomically |

### Example API Requests

**Create a Product**

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

**Record a Sale**

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

### API Response Codes

| Status Code | Meaning |
|---|---|
| `200` | Request successful |
| `400` | Invalid request data |
| `404` | Requested record not found |
| `409` | Stock conflict or database constraint conflict |
| `500` | Unexpected server error |

Unexpected server errors return responses without exposing internal exception details.

### Sales Transaction Integrity

Sales are processed using database transactions.

The following operations are committed together:

1. Sale record creation.
2. Sale item recording.
3. Product stock deduction.
4. Stock movement recording.

If validation fails or there is insufficient stock, the entire transaction is rolled back to prevent inconsistent inventory records.

Sale prices are retrieved from the database rather than accepted from the client.

---

## Data Migration

The checked project did not contain an existing SQLite database file.

The database initializer supports migrating an older SQLite `products` table containing:

- `price`
- `quantity`
- `min_stock_level`

It can add the required Stockroom columns while preserving existing product records and carrying forward their prices and reorder levels.

**PostgreSQL/Supabase Migration**

The application does not automatically import data from the previous PostgreSQL/Supabase configuration.

If existing PostgreSQL data needs to be retained, it must be exported and deliberately migrated into SQLite. The application does not delete or alter the external PostgreSQL database.

---

## Security and Deployment

**Current limitation:** The application does not implement authentication or authorization.

It is currently intended for local testing and development, not public production deployment.

Before deploying publicly, the following improvements are required:

- Implement authentication and authorization.
- Configure HTTPS/TLS.
- Restrict network access appropriately.
- Introduce production logging and monitoring.
- Establish verified database backup procedures.
- Protect database files from public access.

Never expose the SQLite database file or place it inside a publicly accessible static directory.

---

## Project Purpose

Digital Pioneers Stockroom aims to provide small businesses with an affordable and straightforward way to manage their inventory and sales.

By replacing disorganised manual record-keeping with a digital inventory management solution, the project seeks to improve stock visibility, sales tracking, and day-to-day business operations.

---

**Developed by Digital Pioneers**  
*Object-Oriented Programming (OOP) Group Project*
