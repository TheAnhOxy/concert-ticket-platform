-- 1. Bảng Đơn đặt vé (bookings)
CREATE TABLE bookings (
                          id SERIAL PRIMARY KEY,
                          booking_reference VARCHAR(64) UNIQUE NOT NULL,
                          user_id INT NOT NULL,
                          concert_id INT NOT NULL,
                          voucher_id INT NULL,
                          total_amount NUMERIC(12, 2) NOT NULL,
                          discount_amount NUMERIC(12, 2) DEFAULT 0,
                          final_amount NUMERIC(12, 2) NOT NULL,
                          status VARCHAR(30) NOT NULL CHECK (status IN (
                                                                        'RECEIVED', 'WAITING_PAYMENT', 'PAID', 'COMPLETED', 'EXPIRED', 'FAILED', 'CANCELLED'
                              )),
                          idempotency_key VARCHAR(100) UNIQUE NOT NULL, -- Chống duplicate booking khi user retry request
                          expires_at TIMESTAMP NOT NULL, -- Hạn chót thanh toán
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Chi tiết đơn hàng (booking_items)
CREATE TABLE booking_items (
                               id SERIAL PRIMARY KEY,
                               booking_id INT REFERENCES bookings(id) ON DELETE CASCADE,
                               ticket_category_id INT NOT NULL,
                               quantity INT NOT NULL,
                               unit_price NUMERIC(12, 2) NOT NULL,
                               subtotal NUMERIC(12, 2) NOT NULL -- quantity * unit_price
);

-- 3. Bảng giữ chỗ vé tạm thời (ticket_reservations)
CREATE TABLE ticket_reservations (
                                     id SERIAL PRIMARY KEY,
                                     booking_id INT REFERENCES bookings(id) ON DELETE CASCADE,
                                     ticket_category_id INT NOT NULL,
                                     quantity INT NOT NULL,
                                     reserved_until TIMESTAMP NOT NULL,
                                     status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'EXPIRED', 'CONFIRMED')),
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng giữ chỗ Voucher tạm thời (voucher_reservations)
CREATE TABLE voucher_reservations (
                                      id SERIAL PRIMARY KEY,
                                      booking_id INT REFERENCES bookings(id) ON DELETE CASCADE,
                                      voucher_id INT NOT NULL,
                                      reserved_until TIMESTAMP NOT NULL,
                                      status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'EXPIRED', 'CONFIRMED')),
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes tối ưu hiệu suất truy vấn và quét timeout job
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status_expires ON bookings(status, expires_at);
CREATE INDEX idx_reservations_status_time ON ticket_reservations(status, reserved_until);
CREATE INDEX idx_voucher_reservations_status_time ON voucher_reservations(status, reserved_until);

-- 5. Seed dữ liệu mẫu cho bookings & booking_items
INSERT INTO bookings (booking_reference, user_id, concert_id, voucher_id, total_amount, discount_amount, final_amount, status, idempotency_key, expires_at)
VALUES
    ('BKG-20260722-001', 2, 1, NULL, 2000000.00, 0, 2000000.00, 'PAID', 'idemp-key-uuid-001', CURRENT_TIMESTAMP + INTERVAL '10 minutes');

INSERT INTO booking_items (booking_id, ticket_category_id, quantity, unit_price, subtotal)
VALUES
    (1, 1, 2, 1000000.00, 2000000.00);