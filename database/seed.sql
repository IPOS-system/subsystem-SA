USE ipos_sa_db;

START TRANSACTION;

INSERT INTO discount_plan
(discount_plan_id, plan_name, plan_type, fixed_rate, threshold_1, threshold_2, rate_1, rate_2, rate_3, is_active)
VALUES
(1, 'Fixed 5%', 'FIXED', 5.00, NULL, NULL, NULL, NULL, NULL, TRUE),
(2, 'Flexible Monthly', 'FLEXIBLE', NULL, 1000.00, 2000.00, 1.00, 2.00, 3.00, TRUE);

INSERT INTO user_account
(user_id, username, password_hash, role, is_active, created_at)
VALUES
(1, 'admin01',    'hash_admin01',    'ADMIN',    TRUE, '2026-02-01 09:00:00'),
(2, 'manager01',  'hash_manager01',  'MANAGER',  TRUE, '2026-02-01 09:05:00'),
(3, 'greenpharm', 'hash_greenpharm', 'MERCHANT', TRUE, '2026-02-02 10:00:00'),
(4, 'citycare',   'hash_citycare',   'MERCHANT', TRUE, '2026-02-02 10:10:00'),
(5, 'medilink',   'hash_medilink',   'MERCHANT', TRUE, '2026-02-02 10:20:00');

INSERT INTO merchant
(merchant_id, user_id, business_name, contact_name, email, phone,
 address_line_1, address_line_2, city, postcode,
 credit_limit, current_balance, account_state, discount_plan_id, created_at)
VALUES
(1, 3, 'Green Pharmacy', 'Ali Green', 'orders@greenpharmacy.co.uk', '07111 111111',
 '14 High Street', NULL, 'London', 'EC1A 1AA',
 5000.00, 114.00, 'NORMAL', 1, '2026-02-03 11:00:00'),

(2, 4, 'CityCare Chemists', 'Sara Khan', 'accounts@citycare.co.uk', '07222 222222',
 '22 King Road', 'Unit 3', 'Birmingham', 'B1 1BB',
 4000.00, 150.00, 'SUSPENDED', 2, '2026-02-03 11:10:00'),

(3, 5, 'MediLink Stores', 'John Smith', 'sales@medilink.co.uk', '07333 333333',
 '9 Market Lane', NULL, 'Manchester', 'M1 1CC',
 7000.00, 147.50, 'IN_DEFAULT', 2, '2026-02-03 11:20:00');

INSERT INTO catalogue_item
(item_id, product_code, description, unit_price, current_availability, min_stock_level, is_active, last_updated)
VALUES
(1, 'P001', 'Paracetamol 500mg', 2.50, 110, 20, TRUE, '2026-03-10 09:00:00'),
(2, 'P002', 'Ibuprofen 200mg', 3.20, 90, 15, TRUE, '2026-03-10 09:00:00'),
(3, 'P003', 'Cough Syrup 100ml', 4.75, 10, 20, TRUE, '2026-03-10 09:00:00'),
(4, 'P004', 'Vitamin C Tablets', 5.10, 65, 10, TRUE, '2026-03-10 09:00:00'),
(5, 'P005', 'Antiseptic Cream 30g', 3.95, 8, 12, TRUE, '2026-03-10 09:00:00'),
(6, 'P006', 'Allergy Relief Capsules', 6.50, 25, 10, TRUE, '2026-03-10 09:00:00');

INSERT INTO stock_movement
(movement_id, item_id, movement_type, quantity, movement_date, reference_type, reference_id, notes, recorded_by)
VALUES
(1, 1, 'STOCK_IN', 150, '2026-02-10 09:00:00', 'DELIVERY', 1001, 'Initial supplier delivery', 1),
(2, 2, 'STOCK_IN', 100, '2026-02-10 09:10:00', 'DELIVERY', 1002, 'Initial supplier delivery', 1),
(3, 3, 'STOCK_IN', 25, '2026-02-10 09:20:00', 'DELIVERY', 1003, 'Initial supplier delivery', 1),
(4, 4, 'STOCK_IN', 75, '2026-02-10 09:30:00', 'DELIVERY', 1004, 'Initial supplier delivery', 1),
(5, 5, 'STOCK_IN', 20, '2026-02-10 09:40:00', 'DELIVERY', 1005, 'Initial supplier delivery', 1),
(6, 6, 'STOCK_IN', 50, '2026-02-10 09:50:00', 'DELIVERY', 1006, 'Initial supplier delivery', 1),

(7, 1, 'ORDER_OUT', 20, '2026-03-05 12:00:00', 'ORDER', 1, 'Stock removed for accepted order 1', 2),
(8, 2, 'ORDER_OUT', 10, '2026-03-05 12:05:00', 'ORDER', 1, 'Stock removed for accepted order 1', 2),
(9, 4, 'ORDER_OUT', 10, '2026-03-08 14:00:00', 'ORDER', 2, 'Stock removed for dispatched order 2', 2),
(10, 6, 'ORDER_OUT', 10, '2026-03-08 14:05:00', 'ORDER', 2, 'Stock removed for dispatched order 2', 2),
(11, 1, 'ORDER_OUT', 20, '2026-01-20 16:00:00', 'ORDER', 4, 'Stock removed for old overdue order 4', 2),
(12, 6, 'ORDER_OUT', 15, '2026-01-20 16:05:00', 'ORDER', 4, 'Stock removed for old overdue order 4', 2),
(13, 3, 'ADJUSTMENT', 15, '2026-03-01 10:00:00', 'ADJUST', 3001, 'Expired stock written off', 1),
(14, 5, 'ADJUSTMENT', 12, '2026-03-01 10:10:00', 'ADJUST', 3002, 'Damaged stock removed', 1);

INSERT INTO orders
(order_id, merchant_id, order_date, status, subtotal_amount, discount_amount, final_amount,
 dispatched_by, dispatch_date, courier_name, courier_ref_no, expected_delivery_date)
VALUES
(1, 1, '2026-03-05 11:30:00', 'ACCEPTED', 82.00, 4.10, 77.90,
 NULL, NULL, NULL, NULL, NULL),

(2, 1, '2026-03-08 13:30:00', 'DISPATCHED', 116.00, 2.00, 114.00,
 'Warehouse Team A', '2026-03-08 15:00:00', 'DHL', 'DHL-77881', '2026-03-10'),

(3, 2, '2026-02-20 10:45:00', 'PROCESSING', 200.00, 0.00, 200.00,
 NULL, NULL, NULL, NULL, NULL),

(4, 3, '2026-01-20 15:30:00', 'DISPATCHED', 147.50, 0.00, 147.50,
 'Warehouse Team B', '2026-01-20 16:30:00', 'Royal Mail', 'RM-44221', '2026-01-22');

INSERT INTO order_item
(order_item_id, order_id, item_id, quantity, unit_price, line_total)
VALUES
(1, 1, 1, 20, 2.50, 50.00),
(2, 1, 2, 10, 3.20, 32.00),
(3, 2, 4, 10, 5.10, 51.00),
(4, 2, 6, 10, 6.50, 65.00),
(5, 3, 3, 20, 4.75, 95.00),
(6, 3, 5, 20, 3.95, 79.00),
(7, 3, 6, 4, 6.50, 26.00),
(8, 4, 1, 20, 2.50, 50.00),
(9, 4, 6, 15, 6.50, 97.50);


INSERT INTO invoice
(invoice_id, order_id, merchant_id, invoice_date, due_date, gross_amount, discount_amount, amount_due, payment_status)
VALUES
(1, 1, 1, '2026-03-05', '2026-03-31', 82.00, 4.10, 0.00, 'PAID'),
(2, 2, 1, '2026-03-08', '2026-03-31', 116.00, 2.00, 114.00, 'PENDING'),
(3, 3, 2, '2026-02-20', '2026-02-28', 200.00, 0.00, 150.00, 'PARTIAL'),
(4, 4, 3, '2026-01-20', '2026-01-31', 147.50, 0.00, 147.50, 'PENDING');


INSERT INTO payment
(payment_id, merchant_id, invoice_id, payment_date, amount, payment_method, reference_no, entered_by)
VALUES
(1, 1, 1, '2026-03-10', 77.90, 'BANK_TRANSFER', 'BTX1001', 1),
(2, 2, 3, '2026-03-06', 50.00, 'CARD', 'CRD2201', 1);


INSERT INTO audit_log
(log_id, user_id, action_type, entity_name, entity_id, action_time, details)
VALUES
(1, 1, 'CREATE_ACCOUNT', 'merchant', 1, '2026-02-03 11:00:00', 'Created merchant account for Green Pharmacy'),
(2, 1, 'CREATE_ACCOUNT', 'merchant', 2, '2026-02-03 11:10:00', 'Created merchant account for CityCare Chemists'),
(3, 1, 'CREATE_ACCOUNT', 'merchant', 3, '2026-02-03 11:20:00', 'Created merchant account for MediLink Stores'),
(4, 2, 'UPDATE_CREDIT_LIMIT', 'merchant', 2, '2026-02-25 09:30:00', 'Reviewed CityCare credit settings'),
(5, 3, 'PLACE_ORDER', 'orders', 1, '2026-03-05 11:30:00', 'Green Pharmacy placed order 1'),
(6, 1, 'RECORD_PAYMENT', 'payment', 1, '2026-03-10 10:00:00', 'Recorded payment for invoice 1'),
(7, 1, 'RECORD_PAYMENT', 'payment', 2, '2026-03-06 10:15:00', 'Recorded partial payment for invoice 3');

COMMIT;
