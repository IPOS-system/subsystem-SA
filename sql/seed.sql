-- IPOS-SA Sample Data Seed Script

USE ipos_sa;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE payments;
TRUNCATE TABLE invoices;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE monthly_discount_tracker;
TRUNCATE TABLE commercial_applications;
TRUNCATE TABLE catalogue;
TRUNCATE TABLE discount_tiers;
TRUNCATE TABLE merchants;
TRUNCATE TABLE discount_plans;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (user_id, username, password_hash, role, is_active) VALUES
(1,  'Sysdba',     'London_weighting', 'ADMIN',      TRUE),
(2,  'manager',    'Get_it_done', 'MANAGER',    TRUE),
(3,  'accountant', 'Count_money', 'ACCOUNTANT',  TRUE),
(4,  'clerk',      'Paperwork', 'ACCOUNTANT',  TRUE),
(5,  'warehouse1', 'Get_a_beer', 'ADMIN',       TRUE),
(6,  'warehouse2', 'Lot_smell', 'ADMIN',       TRUE),
(7,  'delivery',   'Too_dark', 'ADMIN',       TRUE);


INSERT INTO users (user_id, username, password_hash, role, is_active) VALUES
(8,  'city',    'city_password', 'MERCHANT', TRUE),
(9,  'cosymed', 'cosymed_password', 'MERCHANT', TRUE),
(10, 'hello',   'hello_password', 'MERCHANT', TRUE);

INSERT INTO discount_plans (plan_id, plan_name, plan_type, fixed_rate) VALUES
(1, 'CityPharmacy Fixed 3%', 'FIXED', 3.00);

INSERT INTO discount_plans (plan_id, plan_name, plan_type, fixed_rate) VALUES
(2, 'Cosymed Variable', 'FLEXIBLE', 0.00);

INSERT INTO discount_tiers (plan_id, min_order_val, max_order_val, discount_rate) VALUES
(2, 0.00,    999.99,  0.00),
(2, 1000.00, 2000.00, 1.00),
(2, 2000.01, NULL,    2.00);

INSERT INTO discount_plans (plan_id, plan_name, plan_type, fixed_rate) VALUES
(3, 'HelloPharmacy Variable', 'FLEXIBLE', 0.00);

INSERT INTO discount_tiers (plan_id, min_order_val, max_order_val, discount_rate) VALUES
(3, 0.00,    999.99,  0.00),
(3, 1000.00, 2000.00, 1.00),
(3, 2000.01, NULL,    3.00);


INSERT INTO merchants (merchant_id, user_id, company_name, address, phone, email, credit_limit, current_balance, account_status, status_changed_at, discount_plan_id, authorized_by, created_at) VALUES
(1, 8,  'CityPharmacy',   'Northampton Square, London EC1V 0HB', '0207 040 8000', 'city@citypharmacy.co.uk',     10000.00, 0.00,    'NORMAL',    '2026-02-01 09:00:00', 1, 1, '2026-02-01 09:00:00'),
(2, 9,  'Cosymed Ltd',    '25, Bond Street, London WC1V 8LS',    '0207 321 8001', 'info@cosymed.co.uk',           5000.00, 0.00,    'NORMAL',    '2026-02-01 09:00:00', 2, 1, '2026-02-01 09:00:00'),
(3, 10, 'HelloPharmacy',  '12, Bond Street, London WC1V 9NS',    '0207 321 8002', 'hello@hellopharmacy.co.uk',     5000.00, 1455.00, 'SUSPENDED', '2026-04-15 00:00:00', 3, 1, '2026-02-01 09:00:00');

INSERT INTO catalogue (product_id, description, package_type, unit, units_per_pack, unit_price, availability, min_stock_level, reorder_buffer_pct, is_active) VALUES
('100 00001', 'Paracetamol',           'box',    'Caps', 20, 0.10,  10325, 300, 10.00, TRUE),
('100 00002', 'Aspirin',               'box',    'Caps', 20, 0.50,  12453, 500, 10.00, TRUE),
('100 00003', 'Analgin',               'box',    'Caps', 10, 1.20,  4135,  200, 10.00, TRUE),
('100 00004', 'Celebrex, caps 100 mg', 'box',    'Caps', 10, 10.00, 3410,  200, 10.00, TRUE),
('100 00005', 'Celebrex, caps 200 mg', 'box',    'caps', 10, 18.50, 1440,  150, 10.00, TRUE),
('100 00006', 'Retin-A Tretin, 30 g',  'box',    'caps', 20, 25.00, 2003,  200, 10.00, TRUE),
('100 00007', 'Lipitor TB, 20 mg',     'box',    'caps', 30, 15.50, 1542,  200, 10.00, TRUE),
('100 00008', 'Claritin CR, 60g',      'box',    'caps', 20, 19.50, 2540,  200, 10.00, TRUE),
('200 00004', 'Iodine tincture',       'bottle', 'ml',  100, 0.30,  2094,  200, 10.00, TRUE),
('200 00005', 'Rhynol',                'bottle', 'ml',  200, 2.50,  1878,  300, 10.00, TRUE),
('300 00001', 'Ospen',                 'box',    'caps', 20, 10.50, 766,   200, 10.00, TRUE),
('300 00002', 'Amopen',                'box',    'caps', 30, 15.00, 1250,  300, 10.00, TRUE),
('400 00001', 'Vitamin C',             'box',    'caps', 30, 1.20,  3218,  300, 10.00, TRUE),
('400 00002', 'Vitamin B12',           'box',    'caps', 30, 1.30,  2573,  300, 10.00, TRUE);


INSERT INTO orders (order_id, merchant_id, order_date, status, subtotal, discount_amount, total_amount, dispatched_by_user_id, dispatch_date, courier, courier_ref, expected_delivery) VALUES
('ORD-20260220-0001', 1, '2026-02-20 10:00:00', 'DELIVERED', 508.60, 15.26, 493.34, 7, '2026-02-22 09:00:00', 'InfoPharma Courier', 'IPC-20260220-001', '2026-02-23 15:00:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
('ORD-20260220-0001', '100 00001', 10, 0.10, 1.00),
('ORD-20260220-0001', '100 00003', 20, 1.20, 24.00),
('ORD-20260220-0001', '200 00004', 20, 0.30, 3.60),
('ORD-20260220-0001', '200 00005', 10, 2.50, 25.00),
('ORD-20260220-0001', '300 00001', 10, 10.50, 105.00),
('ORD-20260220-0001', '300 00002', 20, 15.00, 300.00),
('ORD-20260220-0001', '400 00001', 20, 1.20, 24.00),
('ORD-20260220-0001', '400 00002', 20, 1.30, 26.00);

INSERT INTO orders (order_id, merchant_id, order_date, status, subtotal, discount_amount, total_amount, dispatched_by_user_id, dispatch_date, courier, courier_ref, expected_delivery) VALUES
('ORD-20260225-0002', 2, '2026-02-25 10:00:00', 'DELIVERED', 376.00, 0.00, 376.00, 7, '2026-02-25 14:00:00', 'DHL', 'DHL-20260225-001', '2026-02-26 17:00:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
('ORD-20260225-0002', '100 00001', 10, 0.10, 1.00),
('ORD-20260225-0002', '100 00003', 20, 1.20, 24.00),
('ORD-20260225-0002', '200 00005', 10, 2.50, 25.00),
('ORD-20260225-0002', '300 00002', 20, 15.00, 300.00),
('ORD-20260225-0002', '400 00002', 20, 1.30, 26.00);


INSERT INTO orders (order_id, merchant_id, order_date, status, subtotal, discount_amount, total_amount, dispatched_by_user_id, dispatch_date, courier, courier_ref, expected_delivery) VALUES
('ORD-20260225-0003', 3, '2026-02-25 14:00:00', 'DELIVERED', 259.10, 0.00, 259.10, 7, '2026-02-26 09:00:00', 'DHL', 'DHL-20260225-002', '2026-02-27 10:00:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
('ORD-20260225-0003', '100 00003', 20, 1.20, 24.00),
('ORD-20260225-0003', '200 00004', 20, 0.30, 3.60),
('ORD-20260225-0003', '300 00001', 3,  10.50, 31.50),
('ORD-20260225-0003', '300 00002', 10, 15.00, 150.00),
('ORD-20260225-0003', '400 00001', 20, 1.20, 24.00),
('ORD-20260225-0003', '400 00002', 20, 1.30, 26.00);


INSERT INTO orders (order_id, merchant_id, order_date, status, subtotal, discount_amount, total_amount, dispatched_by_user_id, dispatch_date, courier, courier_ref, expected_delivery) VALUES
('ORD-20260310-0004', 2, '2026-03-10 10:00:00', 'DELIVERED', 430.00, 0.00, 430.00, 7, '2026-03-11 09:00:00', 'InfoPharma Courier', 'IPC-20260310-001', '2026-03-12 11:00:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
('ORD-20260310-0004', '200 00005', 10, 2.50, 25.00),
('ORD-20260310-0004', '300 00001', 10, 10.50, 105.00),
('ORD-20260310-0004', '300 00002', 20, 15.00, 300.00);


INSERT INTO orders (order_id, merchant_id, order_date, status, subtotal, discount_amount, total_amount, dispatched_by_user_id, dispatch_date, courier, courier_ref, expected_delivery) VALUES
('ORD-20260325-0005', 3, '2026-03-25 10:00:00', 'DELIVERED', 877.50, 0.00, 877.50, 7, '2026-03-26 09:00:00', 'InfoPharma Courier', 'IPC-20260325-001', '2026-03-27 10:00:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
('ORD-20260325-0005', '100 00003', 20, 1.20, 24.00),
('ORD-20260325-0005', '100 00004', 5,  10.00, 50.00),
('ORD-20260325-0005', '100 00005', 5,  18.50, 92.50),
('ORD-20260325-0005', '100 00006', 5,  25.00, 125.00),
('ORD-20260325-0005', '100 00007', 10, 15.50, 155.00),
('ORD-20260325-0005', '300 00001', 10, 10.50, 105.00),
('ORD-20260325-0005', '300 00002', 20, 15.00, 300.00),
('ORD-20260325-0005', '400 00002', 20, 1.30, 26.00);


INSERT INTO orders (order_id, merchant_id, order_date, status, subtotal, discount_amount, total_amount, dispatched_by_user_id, dispatch_date, courier, courier_ref, expected_delivery) VALUES
('ORD-20260401-0006', 3, '2026-04-01 10:00:00', 'DELIVERED', 577.50, 0.00, 577.50, 7, '2026-04-02 09:00:00', 'InfoPharma Courier', 'IPC-20260401-001', '2026-04-03 10:00:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
('ORD-20260401-0006', '100 00003', 20, 1.20, 24.00),
('ORD-20260401-0006', '100 00004', 5,  10.00, 50.00),
('ORD-20260401-0006', '100 00005', 5,  18.50, 92.50),
('ORD-20260401-0006', '100 00006', 5,  25.00, 125.00),
('ORD-20260401-0006', '100 00007', 10, 15.50, 155.00),
('ORD-20260401-0006', '300 00001', 10, 10.50, 105.00),
('ORD-20260401-0006', '400 00002', 20, 1.30, 26.00);


INSERT INTO invoices (invoice_id, order_id, merchant_id, invoice_date, amount_due, payment_status, due_date) VALUES
('INV-ORD-20260220-0001', 'ORD-20260220-0001', 1, '2026-02-20 10:00:00', 493.34, 'PAID',    '2026-02-28'),
('INV-ORD-20260225-0002', 'ORD-20260225-0002', 2, '2026-02-25 10:00:00', 376.00, 'PAID',    '2026-02-28'),
('INV-ORD-20260225-0003', 'ORD-20260225-0003', 3, '2026-02-25 14:00:00', 259.10, 'PAID',    '2026-02-28'),
('INV-ORD-20260310-0004', 'ORD-20260310-0004', 2, '2026-03-10 10:00:00', 430.00, 'PAID',    '2026-03-31'),
('INV-ORD-20260325-0005', 'ORD-20260325-0005', 3, '2026-03-25 10:00:00', 877.50, 'OVERDUE', '2026-03-31'),
('INV-ORD-20260401-0006', 'ORD-20260401-0006', 3, '2026-04-01 10:00:00', 577.50, 'PENDING', '2026-04-30');


INSERT INTO payments (merchant_id, invoice_id, amount_paid, payment_method, payment_date, recorded_by, notes) VALUES
(1, 'INV-ORD-20260220-0001', 493.34, 'BANK_TRANSFER', '2026-03-15 10:00:00', 3, 'Full payment - bank transfer');

INSERT INTO payments (merchant_id, invoice_id, amount_paid, payment_method, payment_date, recorded_by, notes) VALUES
(2, 'INV-ORD-20260225-0002', 376.00, 'CARD', '2026-03-15 11:00:00', 3, 'Full payment - company credit card'),
(2, 'INV-ORD-20260310-0004', 430.00, 'CARD', '2026-03-15 11:00:00', 3, 'Full payment - company credit card');

INSERT INTO payments (merchant_id, invoice_id, amount_paid, payment_method, payment_date, recorded_by, notes) VALUES
(3, 'INV-ORD-20260225-0003', 259.10, 'BANK_TRANSFER', '2026-03-05 10:00:00', 3, 'Full payment - cleared Feb balance');

INSERT INTO audit_log (user_id, action, target_type, target_id, details, logged_at) VALUES
(1, 'CREATE_MERCHANT', 'MERCHANT', '1', 'Created merchant account: CityPharmacy', '2026-02-01 09:00:00'),
(1, 'CREATE_MERCHANT', 'MERCHANT', '2', 'Created merchant account: Cosymed Ltd', '2026-02-01 09:05:00'),
(1, 'CREATE_MERCHANT', 'MERCHANT', '3', 'Created merchant account: HelloPharmacy', '2026-02-01 09:10:00'),
(1, 'ACCOUNT_STATUS_CHANGE', 'MERCHANT', '3', 'HelloPharmacy status changed to SUSPENDED - overdue invoice INV-ORD-20260325-0005', '2026-04-15 00:00:00');

