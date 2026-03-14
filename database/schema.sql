CREATE DATABASE IF NOT EXISTS ipos_sa_db;
USE ipos_sa_db;

CREATE TABLE user_account (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (role IN ('MERCHANT', 'ADMIN', 'MANAGER', 'ACCOUNTING'))
) ENGINE=InnoDB;

CREATE TABLE discount_plan (
    discount_plan_id INT AUTO_INCREMENT PRIMARY KEY,
    plan_name VARCHAR(50) NOT NULL UNIQUE,
    plan_type VARCHAR(20) NOT NULL,
    fixed_rate DECIMAL(5,2) NULL,
    threshold_1 DECIMAL(10,2) NULL,
    threshold_2 DECIMAL(10,2) NULL,
    rate_1 DECIMAL(5,2) NULL,
    rate_2 DECIMAL(5,2) NULL,
    rate_3 DECIMAL(5,2) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (plan_type IN ('FIXED', 'FLEXIBLE')),
    CHECK (fixed_rate IS NULL OR fixed_rate >= 0),
    CHECK (threshold_1 IS NULL OR threshold_1 >= 0),
    CHECK (threshold_2 IS NULL OR threshold_2 >= 0),
    CHECK (rate_1 IS NULL OR rate_1 >= 0),
    CHECK (rate_2 IS NULL OR rate_2 >= 0),
    CHECK (rate_3 IS NULL OR rate_3 >= 0)
) ENGINE=InnoDB;

CREATE TABLE merchant (
    merchant_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    business_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address_line_1 VARCHAR(120) NOT NULL,
    address_line_2 VARCHAR(120) NULL,
    city VARCHAR(60) NOT NULL,
    postcode VARCHAR(20) NOT NULL,
    credit_limit DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    current_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    account_state VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    discount_plan_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (credit_limit >= 0),
    CHECK (current_balance >= 0),
    CHECK (account_state IN ('NORMAL', 'SUSPENDED', 'IN_DEFAULT')),
    CONSTRAINT fk_merchant_user
        FOREIGN KEY (user_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_merchant_discount_plan
        FOREIGN KEY (discount_plan_id) REFERENCES discount_plan(discount_plan_id)
) ENGINE=InnoDB;

CREATE TABLE catalogue_item (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    current_availability INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (unit_price >= 0),
    CHECK (current_availability >= 0),
    CHECK (min_stock_level >= 0)
) ENGINE=InnoDB;

CREATE TABLE stock_movement (
    movement_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    movement_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_type VARCHAR(20) NULL,
    reference_id INT NULL,
    notes VARCHAR(255) NULL,
    recorded_by INT NULL,
    CHECK (movement_type IN ('STOCK_IN', 'ORDER_OUT', 'ADJUSTMENT')),
    CHECK (quantity > 0),
    CONSTRAINT fk_stock_item
        FOREIGN KEY (item_id) REFERENCES catalogue_item(item_id),
    CONSTRAINT fk_stock_recorded_by
        FOREIGN KEY (recorded_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    merchant_id INT NOT NULL,
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    subtotal_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(10,2) NOT NULL,
    dispatched_by VARCHAR(100) NULL,
    dispatch_date DATETIME NULL,
    courier_name VARCHAR(100) NULL,
    courier_ref_no VARCHAR(50) NULL,
    expected_delivery_date DATE NULL,
    CHECK (status IN ('ACCEPTED', 'PROCESSING', 'DISPATCHED')),
    CHECK (subtotal_amount >= 0),
    CHECK (discount_amount >= 0),
    CHECK (final_amount >= 0),
    CONSTRAINT fk_orders_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant(merchant_id)
) ENGINE=InnoDB;

CREATE TABLE order_item (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (line_total >= 0),
    CONSTRAINT uq_order_item UNIQUE (order_id, item_id),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_order_item_item
        FOREIGN KEY (item_id) REFERENCES catalogue_item(item_id)
) ENGINE=InnoDB;

CREATE TABLE invoice (
    invoice_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    merchant_id INT NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    amount_due DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CHECK (gross_amount >= 0),
    CHECK (discount_amount >= 0),
    CHECK (amount_due >= 0),
    CHECK (payment_status IN ('PENDING', 'PARTIAL', 'PAID')),
    CONSTRAINT fk_invoice_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_invoice_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant(merchant_id)
) ENGINE=InnoDB;

CREATE TABLE payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    merchant_id INT NOT NULL,
    invoice_id INT NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    reference_no VARCHAR(50) NULL,
    entered_by INT NULL,
    CHECK (amount > 0),
    CHECK (payment_method IN ('BANK_TRANSFER', 'CARD', 'CHEQUE', 'CASH')),
    CONSTRAINT fk_payment_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant(merchant_id),
    CONSTRAINT fk_payment_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoice(invoice_id),
    CONSTRAINT fk_payment_entered_by
        FOREIGN KEY (entered_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB;

CREATE TABLE audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id INT NOT NULL,
    action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT NULL,
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB;
