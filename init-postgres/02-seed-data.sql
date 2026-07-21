-- --- SEED CHO AUTH_DB ---
\c auth_db;

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100),
                       phone_number VARCHAR(20),
                       role VARCHAR(20) NOT NULL CHECK (role IN ('ROLE_CUSTOMER', 'ROLE_OPERATOR')),
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (id, username, email, password_hash, full_name, phone_number, role) VALUES
                                                                                          (1, 'operator_admin', 'admin@geekup.vn', '$2a$10$dummyhash', 'System Operator', '0901234567', 'ROLE_OPERATOR'),
                                                                                          (2, 'nguyentheanh', 'theanh.dev@gmail.com', '$2a$10$dummyhash', 'Nguyễn Thế Anh', '0987654321', 'ROLE_CUSTOMER');


-- --- SEED CHO CORE_DB ---
\c core_db;

CREATE TABLE concerts (
                          id SERIAL PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          venue VARCHAR(255) NOT NULL,
                          start_time TIMESTAMP NOT NULL,
                          status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'FINISHED')),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ticket_categories (
                                   id SERIAL PRIMARY KEY,
                                   concert_id INT REFERENCES concerts(id) ON DELETE CASCADE,
                                   name VARCHAR(100) NOT NULL,
                                   price NUMERIC(12, 2) NOT NULL,
                                   total_quantity INT NOT NULL,
                                   available_quantity INT NOT NULL
);

CREATE TABLE vouchers (
                          id SERIAL PRIMARY KEY,
                          code VARCHAR(50) UNIQUE NOT NULL,
                          discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
                          discount_value NUMERIC(12, 2) NOT NULL,
                          min_order_value NUMERIC(12, 2) DEFAULT 0,
                          max_discount_amount NUMERIC(12, 2),
                          usage_limit INT NOT NULL,
                          start_time TIMESTAMP NOT NULL,
                          end_time TIMESTAMP NOT NULL
);

INSERT INTO concerts (id, title, description, venue, start_time, status) VALUES
    (1, 'Gió Concert - Flash Sale Launch', 'Đêm nhạc hội đặc biệt.', 'Quân Khu 7, TP.HCM', '2026-08-20 20:00:00', 'PUBLISHED');

INSERT INTO ticket_categories (id, concert_id, name, price, total_quantity, available_quantity) VALUES
                                                                                                    (1, 1, 'VIP VVIP', 2000000.00, 100, 100),
                                                                                                    (2, 1, 'Standard Zone A', 800000.00, 500, 500);

INSERT INTO vouchers (id, code, discount_type, discount_value, min_order_value, usage_limit, start_time, end_time) VALUES
    (1, 'GEEKUP2026', 'PERCENTAGE', 15.00, 500000.00, 50, '2026-07-01 00:00:00', '2026-08-30 23:59:59');


-- --- SEED CHO BOOKING_DB ---
\c booking_db;

CREATE TABLE bookings (
                          id SERIAL PRIMARY KEY,
                          booking_reference VARCHAR(64) UNIQUE NOT NULL,
                          user_id INT NOT NULL,
                          concert_id INT NOT NULL,
                          voucher_id INT NULL,
                          total_amount NUMERIC(12, 2) NOT NULL,
                          discount_amount NUMERIC(12, 2) DEFAULT 0,
                          final_amount NUMERIC(12, 2) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          idempotency_key VARCHAR(100) UNIQUE NOT NULL,
                          expires_at TIMESTAMP NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_items (
                               id SERIAL PRIMARY KEY,
                               booking_id INT REFERENCES bookings(id) ON DELETE CASCADE,
                               ticket_category_id INT NOT NULL,
                               quantity INT NOT NULL,
                               unit_price NUMERIC(12, 2) NOT NULL,
                               subtotal NUMERIC(12, 2) NOT NULL
);

CREATE TABLE ticket_reservations (
                                     id SERIAL PRIMARY KEY,
                                     booking_id INT REFERENCES bookings(id) ON DELETE CASCADE,
                                     ticket_category_id INT NOT NULL,
                                     quantity INT NOT NULL,
                                     reserved_until TIMESTAMP NOT NULL,
                                     status VARCHAR(20) NOT NULL
);


-- --- SEED CHO PAYMENT_DB ---
\c payment_db;

CREATE TABLE payments (
                          id SERIAL PRIMARY KEY,
                          booking_reference VARCHAR(64) NOT NULL,
                          user_id INT NOT NULL,
                          amount NUMERIC(12, 2) NOT NULL,
                          payment_method VARCHAR(30) NOT NULL,
                          transaction_id VARCHAR(100) UNIQUE,
                          status VARCHAR(30) NOT NULL,
                          callback_data TEXT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);