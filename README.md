# Digital Pioneers
## Small Business Stock Management System

A lightweight, mobile-friendly stock management application designed to help small businesses manage inventory, record sales, and monitor stock levels.

---

## About the Project

Small businesses often struggle to maintain accurate stock records, track products sold, and identify items that need restocking. Manual record-keeping can lead to errors, stock shortages, and difficulties monitoring available inventory.

The **Small Business Stock Management System** aims to address these challenges by providing a simple and affordable digital solution that works on smartphones and computers, including low-cost devices.

## Project Objectives

- Simplify product and inventory management.
- Improve stock record accuracy.
- Record sales and automatically update stock quantities.
- Identify products that require restocking.
- Provide an accessible and easy-to-use interface for small businesses.

## Key Features

The initial version will include:

- **Product Management:** Register, view, update, and delete products.
- **Inventory Tracking:** Monitor available product quantities.
- **Sales Management:** Record sales and automatically reduce stock.
- **Stock Replenishment:** Record newly received stock.
- **Low-Stock Alerts:** Identify products that have reached their minimum stock levels.
- **Responsive Interface:** Support smartphones and computers.

## Technologies Used

| Technology | Purpose |
|---|---|
| Java | Backend development |
| Javalin | REST API and web server |
| SQLite | Database and persistent storage |
| HTML | Frontend structure |
| CSS | Styling and responsive design |
| JavaScript | Frontend functionality and API communication |
| Maven | Dependency and project management |
| Git & GitHub | Version control and collaboration |

## Object-Oriented Programming Concepts

The project demonstrates the following OOP concepts:

- **Encapsulation:** Protecting class attributes using private access modifiers.
- **Inheritance / Interfaces:** Using interfaces to define common stock operations.
- **Polymorphism:** Implementing stock operations differently for sales and stock replenishment.
- **Abstraction:** Separating system operations into classes and interfaces.
- **Custom Exceptions:** Handling invalid operations, such as attempting to sell more stock than is available.
- **Collections:** Managing products and stock records using Java collections.

## Main Classes

| Class | Responsibility |
|---|---|
| Product | Stores product information. |
| Inventory | Manages products and stock quantities. |
| Sale | Records sales transactions. |
| Supplier | Stores supplier information. |
| StockRestock | Handles stock replenishment. |
| StockAlert | Identifies low-stock products. |
| StockManagementService | Coordinates stock operations. |

## Project Structure

The proposed project structure is:

```text
Digital-Pioneers-Stock-Management/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/digitalpioneers/
│       │       ├── models/
│       │       ├── services/
│       │       ├── database/
│       │       ├── exceptions/
│       │       └── Main.java
│       │
│       └── resources/
│           ├── public/
│           │   ├── index.html
│           │   ├── css/
│           │   └── js/
│           └── database/
│
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

Install the following:

- Java Development Kit (JDK).
- Apache Maven.
- Git.

### Installation

1. Clone the repository:

```bash
git clone https://github.com/YOUR-USERNAME/YOUR-REPOSITORY.git
```

2. Navigate into the project directory:

```bash
cd YOUR-REPOSITORY
```

3. Build the project:

```bash
mvn clean package
```

4. Run the application using the configured Maven command or Java entry point.

*Note: Setup and run commands may be updated as development progresses.*

## Project Team

**Group Name:** Digital Pioneers

| Member | Responsibility |
|---|---|
| Member 1 | Product and Inventory Management |
| Member 2 | Sales and Stock Operations |
| Member 3 | Database and API Integration |
| Member 4 | Frontend Development and Testing |

## Project Status

**Status:** Under Development

This project is being developed as part of an Object-Oriented Programming (OOP) group assignment.

## Future Improvements

Potential future additions include:

- Barcode scanning.
- Sales reports and analytics.
- Product expiry tracking.
- Multiple user accounts and access levels.
- Multiple branch support.

## License

This project is developed for academic purposes by Digital Pioneers.

---

**Digital Pioneers — Building practical software solutions for everyday business challenges.**
