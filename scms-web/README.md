# Smart Canteen Management System — Spring Boot Web App

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 17 |
| Database | MySQL 8+ (JPA/Hibernate auto-creates schema) |
| Security | Spring Security (BCrypt, role-based access) |
| Frontend | Thymeleaf + custom dark CSS |
| Build | Maven 3.8+ |

---

## Quick Start (5 steps)

### Step 1 — Prerequisites
- Java 17+ → https://adoptium.net
- Maven 3.8+ → https://maven.apache.org/download.cgi
- MySQL 8+ → https://dev.mysql.com/downloads/mysql/

### Step 2 — Create MySQL Database
```sql
CREATE DATABASE scms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Or just let Spring Boot create it automatically (it will if your user has CREATE privileges).

### Step 3 — Configure Database Password
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 4 — Run the Application
```bash
cd scms-web
mvn spring-boot:run
```
Wait for: `Started ScmsApplication in X.X seconds`

### Step 5 — Open in Browser
```
http://localhost:8080
```

---

## Demo Credentials (auto-seeded on first run)

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@scms.com | admin123 |
| Staff | staff@scms.com | staff123 |
| Inventory Manager | stock@scms.com | stock123 |
| Customer | arjun@scms.com | pass123 |

---

## Features by Role

### 👑 Admin
- Analytics dashboard (revenue charts, order stats)
- Full menu management (add/edit/delete items, map ingredients)
- Inventory management (add ingredients, restock, track low stock)
- Coupon management (flat/percentage discounts)
- All orders view with cancel capability
- User management (activate/deactivate accounts)
- Feedback moderation (flag reviews)
- Access to Staff and Customer views

### 👨‍🍳 Staff
- Live kitchen queue (auto-refreshes every 30s)
- Priority-sorted active orders
- One-click: Start Preparing → Mark Ready → Mark Collected

### 📦 Inventory Manager
- Stock level dashboard with visual bars
- Quick restock directly from the dashboard
- Low-stock alerts

### 🛒 Customer
- Browse menu with search, category filters, dietary tags
- Add items with quantity controls
- Apply coupon codes at checkout
- Choose payment: Wallet, UPI, Card, Cash
- Real-time order status tracker
- Order history with detail view
- Submit feedback/ratings per order or per item
- Wallet top-up with preset amounts

---

## Project Structure
```
scms-web/
├── pom.xml
└── src/main/
    ├── java/com/scms/
    │   ├── ScmsApplication.java
    │   ├── model/              # JPA entities (User, MenuItem, Order, ...)
    │   ├── model/enums/        # Role, OrderStatus, PaymentMethod
    │   ├── repository/         # Spring Data JPA repositories
    │   ├── service/            # Business logic
    │   ├── controller/         # Web MVC controllers
    │   └── config/             # Security, DataSeeder, GlobalModelAdvice
    └── resources/
        ├── application.properties
        ├── static/css/main.css
        ├── static/js/main.js
        └── templates/
            ├── layout.html
            ├── auth/
            ├── admin/
            ├── staff/
            ├── customer/
            └── inventory/
```

---

## How the Database Works
- Spring Boot auto-creates all tables on first run (`spring.jpa.hibernate.ddl-auto=update`)
- `DataSeeder.java` seeds demo data (users, ingredients, menu items, coupons) on first run
- All subsequent runs skip seeding (checks `userRepo.count() > 0`)
- Tables: `users`, `menu_items`, `menu_item_ingredients`, `ingredients`, `orders`, `order_items`, `coupons`, `feedbacks`

---

## Order State Machine
```
CREATED → CONFIRMED → PREPARING → READY → COMPLETED
    └──────────────────────────────────── CANCELLED → REFUNDED
```
Invalid transitions throw `IllegalStateException` (enforced in the `OrderStatus` enum).

---

## Troubleshooting

**Port already in use:**
```properties
server.port=8081
```

**MySQL connection refused:**
- Make sure MySQL service is running: `net start MySQL80` (Windows) or `sudo service mysql start` (Linux)

**Wrong password error:**
- Edit `application.properties` → set correct `spring.datasource.password`

**Tables not created:**
- Ensure your MySQL user has CREATE TABLE privileges
- Or run manually: `GRANT ALL PRIVILEGES ON scms_db.* TO 'root'@'localhost';`
