

CREATE DATABASE IF NOT EXISTS ipos_sa;
USE ipos_sa;

CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','MANAGER','ACCOUNTANT','MERCHANT') NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS merchants (
    merchant_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT          NOT NULL UNIQUE,
    company_name    VARCHAR(100) NOT NULL,
    address         VARCHAR(255),
    phone           VARCHAR(30),
    fax             VARCHAR(30),
    email           VARCHAR(100),
    credit_limit    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    current_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    account_status  ENUM('NORMAL','SUSPENDED','IN_DEFAULT') NOT NULL DEFAULT 'NORMAL',
    discount_plan_id INT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS discount_plans (
    plan_id     INT AUTO_INCREMENT PRIMARY KEY,
    plan_name   VARCHAR(100) NOT NULL,
    plan_type   ENUM('FIXED','FLEXIBLE') NOT NULL,
    -- For FIXED: fixed_rate holds the single discount %
    fixed_rate  DECIMAL(5,2) DEFAULT 0.00,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discount_tiers (
    tier_id       INT AUTO_INCREMENT PRIMARY KEY,
    plan_id       INT           NOT NULL,
    min_order_val DECIMAL(10,2) NOT NULL,   -- lower bound of tier
    max_order_val DECIMAL(10,2),            -- NULL = no upper bound
    discount_rate DECIMAL(5,2)  NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES discount_plans(plan_id) ON DELETE CASCADE
);

ALTER TABLE merchants
    ADD CONSTRAINT fk_merchant_plan
    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(plan_id);

CREATE TABLE IF NOT EXISTS catalogue (
    product_id        VARCHAR(20)  PRIMARY KEY,   -- e.g. "100 00001"
    description       VARCHAR(200) NOT NULL,
    package_type      VARCHAR(50),
    unit              VARCHAR(20),
    units_per_pack    INT          NOT NULL DEFAULT 1,
    unit_price        DECIMAL(10,2) NOT NULL,
    availability      INT          NOT NULL DEFAULT 0,  -- packs in stock
    min_stock_level   INT          NOT NULL DEFAULT 0,
    reorder_buffer_pct DECIMAL(5,2) NOT NULL DEFAULT 10.00, -- % above min to order up to
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    order_id        VARCHAR(20)   PRIMARY KEY,   -- e.g. "IP2034"
    merchant_id     INT           NOT NULL,
    order_date      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('ACCEPTED','PROCESSING','DISPATCHED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'ACCEPTED',
    subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    -- Dispatch info (filled when dispatched)
    dispatched_by   VARCHAR(100),
    dispatch_date   DATETIME,
    courier         VARCHAR(100),
    courier_ref     VARCHAR(100),
    expected_delivery DATETIME,
    FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id)
);

CREATE TABLE IF NOT EXISTS order_items (
    item_id     INT AUTO_INCREMENT PRIMARY KEY,
    order_id    VARCHAR(20)   NOT NULL,
    product_id  VARCHAR(20)   NOT NULL,
    quantity    INT           NOT NULL,
    unit_price  DECIMAL(10,2) NOT NULL,
    line_total  DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(order_id)   ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES catalogue(product_id)
);

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id    VARCHAR(20)   PRIMARY KEY,   -- e.g. "INV-2026-00031"
    order_id      VARCHAR(20)   NOT NULL UNIQUE,
    merchant_id   INT           NOT NULL,
    invoice_date  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount_due    DECIMAL(10,2) NOT NULL,
    payment_status ENUM('PENDING','PAID','OVERDUE') NOT NULL DEFAULT 'PENDING',
    due_date      DATE          NOT NULL,
    FOREIGN KEY (order_id)    REFERENCES orders(order_id),
    FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    merchant_id     INT           NOT NULL,
    invoice_id      VARCHAR(20)   NOT NULL,
    amount_paid     DECIMAL(10,2) NOT NULL,
    payment_method  ENUM('BANK_TRANSFER','CARD','CHEQUE') NOT NULL,
    payment_date    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by     INT           NOT NULL,   -- user_id of accountant
    notes           TEXT,
    FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    FOREIGN KEY (invoice_id)  REFERENCES invoices(invoice_id),
    FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS stock_movement (
    movement_id     INT AUTO_INCREMENT PRIMARY KEY,
    product_id      VARCHAR(20) NOT NULL,
    movement_type   ENUM('STOCK_IN','ORDER_OUT','ADJUSTMENT') NOT NULL,
    quantity        INT NOT NULL,
    movement_date   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_type  VARCHAR(50),
    reference_id    INT,
    notes           VARCHAR(500),
    recorded_by     INT,
    FOREIGN KEY (product_id) REFERENCES catalogue(product_id),
    FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),            -- e.g. "MERCHANT", "ORDER"
    target_id   VARCHAR(50),            -- e.g. merchant_id or order_id
    details     TEXT,
    logged_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

INSERT IGNORE INTO users (username, password_hash, role)
VALUES ('admin',
        SHA2('Admin1234!', 256),
        'ADMIN');
