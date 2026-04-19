# IPOS-SA – Team 16 (GoatFlow)

This project is the **IPOS Server Application (IPOS-SA)** for the InfoPharma Ordering System.

It includes:
- a **Spring Boot backend** for business logic and database access
- a **WPF desktop frontend** for the user interface
- **MySQL scripts** for database setup and seed data

## Main features
- user login and role-based access
- merchant account management
- catalogue and stock management
- order placement and tracking
- invoice and payment handling
- reports and audit log
- commercial application handling

## Technologies used
- **Backend:** Java 21, Spring Boot, Maven
- **Frontend:** C# WPF, .NET 9, Visual Studio 2022
- **Database:** MySQL 8

## Project structure
- `Backend/` – Spring Boot backend
- `Frontend/` – WPF desktop application
- `sql/` – database schema and seed data

## Requirements
Before running the project, install:
- Java 21
- MySQL 8
- Windows
- .NET 9 SDK
- Visual Studio 2022

## Database setup
Run these commands in MySQL:

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/seed_data.sql
```

## Backend setup
1. Open `Backend/src/main/resources/application.properties`
2. Check the MySQL username and password
3. Start the backend:

```bash
cd Backend
./mvnw spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

## Frontend setup
1. Open `Frontend/GOATflow.sln` in Visual Studio 2022
2. Run the project with **F5**

## Default login accounts
### Admin
- Username: `Sysdba`
- Password: `London_weighting`

### Manager
- Username: `manager`
- Password: `Get_it_done`

### Accountant
- Username: `accountant`
- Password: `Count_money`

### Merchant
- Username: `city`
- Password: `city_password`

## Running tests
Backend unit tests can be run with:

```bash
cd Backend
./mvnw test
```

## Notes
- Make sure MySQL is running before starting the backend.
- The frontend requires the backend to be running on port `8080`.
- Seed data creates sample users, merchants, catalogue items, orders, invoices, and payments.

