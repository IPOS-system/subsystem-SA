
--  Version: 2.0 (Updated based on Deliverable 1 feedback)

CREATE DATABASE IF NOT EXISTS ipos_sa;
USE ipos_sa;

CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','MANAGER','ACCOUNTANT','DIRECTOR','MERCHANT') NOT NULL,
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
    status_changed_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    discount_plan_id    INT,
    authorized_by       INT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (authorized_by) REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS discount_plans (
    plan_id     INT AUTO_INCREMENT PRIMARY KEY,
    plan_name   VARCHAR(100) NOT NULL,
    plan_type   ENUM('FIXED','FLEXIBLE') NOT NULL,
    fixed_rate  DECIMAL(5,2) DEFAULT 0.00,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discount_tiers (
    tier_id       INT AUTO_INCREMENT PRIMARY KEY,
    plan_id       INT           NOT NULL,
    min_order_val DECIMAL(10,2) NOT NULL,  
    max_order_val DECIMAL(10,2),           
    discount_rate DECIMAL(5,2)  NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES discount_plans(plan_id) ON DELETE CASCADE
);

ALTER TABLE merchants
    ADD CONSTRAINT fk_merchant_plan
    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(plan_id);

CREATE TABLE IF NOT EXISTS catalogue (
    product_id        VARCHAR(20)  PRIMARY KEY,  
    description       VARCHAR(200) NOT NULL,
    package_type      VARCHAR(50),
    unit              VARCHAR(20),
    units_per_pack    INT          NOT NULL DEFAULT 1,
    unit_price        DECIMAL(10,2) NOT NULL,
    availability      INT          NOT NULL DEFAULT 0,  
    min_stock_level   INT          NOT NULL DEFAULT 0,
    reorder_buffer_pct DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS orders (
    order_id        VARCHAR(20)   PRIMARY KEY,   
    merchant_id     INT           NOT NULL,
    order_date      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('ACCEPTED','PROCESSING','DISPATCHED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'ACCEPTED',
    subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    dispatched_by_user_id INT DEFAULT NULL,
    dispatch_date   DATETIME,
    courier         VARCHAR(100),
    courier_ref     VARCHAR(100),
    expected_delivery DATETIME,
    FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    FOREIGN KEY (dispatched_by_user_id) REFERENCES users(user_id)
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
    invoice_id    VARCHAR(20)   PRIMARY KEY,  
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


CREATE TABLE IF NOT EXISTS monthly_discount_tracker (
    tracker_id      INT AUTO_INCREMENT PRIMARY KEY,
    merchant_id     INT           NOT NULL,
    year_month      CHAR(7)       NOT NULL,   
    total_order_value DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_rate   DECIMAL(5,2)  DEFAULT NULL,  
    discount_amount DECIMAL(10,2) DEFAULT NULL,  
    settled         BOOLEAN NOT NULL DEFAULT FALSE,
    settled_method  ENUM('CHEQUE','DEDUCTED_FROM_ORDER') DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id),
    UNIQUE KEY uq_merchant_month (merchant_id, year_month)
);


CREATE TABLE IF NOT EXISTS audit_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),            
    target_id   VARCHAR(50),          
    details     TEXT,
    logged_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS commercial_applications (
    application_id  INT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100) NOT NULL,
    company_reg_no  VARCHAR(50)  NOT NULL,
    company_name    VARCHAR(100) NOT NULL,
    directors       VARCHAR(255) NOT NULL,
    business_type   VARCHAR(100),
    address         VARCHAR(255),
    status          ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by     INT DEFAULT NULL,
    submitted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME DEFAULT NULL,
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);


INSERT IGNORE INTO users (username, password_hash, role)
VALUES ('admin',
        SHA2('Admin1234!', 256),
        'ADMIN');

INSERT IGNORE INTO users (username, password_hash, role)
VALUES ('director',
        SHA2('Director1234!', 256),
        'DIRECTOR');
