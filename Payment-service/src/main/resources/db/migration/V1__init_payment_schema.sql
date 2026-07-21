-- 1. Tạo bảng payments cho payment_db
CREATE TABLE payments (
                          id SERIAL PRIMARY KEY,
                          booking_reference VARCHAR(64) NOT NULL,
                          user_id INT NOT NULL,
                          amount NUMERIC(12, 2) NOT NULL,
                          payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('MOCK', 'VNPAY', 'MOMO', 'PAYPAL')),
                          transaction_id VARCHAR(100) UNIQUE,
                          status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
                          callback_data TEXT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tạo Index tối ưu tra cứu
CREATE INDEX idx_payments_reference ON payments(booking_reference);

-- 3. Seed dữ liệu mẫu cho payments
INSERT INTO payments (booking_reference, user_id, amount, payment_method, transaction_id, status, callback_data)
VALUES
    ('BKG-20260722-001', 2, 2000000.00, 'MOCK', 'TXN-MOCK-999888', 'SUCCESS', '{"vnp_ResponseCode": "00", "message": "Confirm Success"}'),
    ('BKG-20260722-002', 2, 800000.00, 'VNPAY', NULL, 'PENDING', NULL);