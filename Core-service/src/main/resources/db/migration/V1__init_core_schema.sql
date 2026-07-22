-- 1. Bảng Sự kiện / Đại nhạc hội (concerts)
CREATE TABLE concerts (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'FINISHED')
    ),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Hạng vé của sự kiện (ticket_categories)
CREATE TABLE ticket_categories (
    id SERIAL PRIMARY KEY,
    concert_id INT REFERENCES concerts (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL, -- VIP, Standard, v.v.
    price NUMERIC(12, 2) NOT NULL,
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL, -- Số lượng thực tế còn lại trong kho DB
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng Mã giảm giá (vouchers)
CREATE TABLE vouchers (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value NUMERIC(12, 2) NOT NULL, -- Giá trị giảm (VD: 10 (%) hoặc 50000 (VND))
    min_order_value NUMERIC(12, 2) DEFAULT 0,
    max_discount_amount NUMERIC(12, 2), -- Giới hạn mức giảm tối đa (nếu giảm theo %)
    usage_limit INT NOT NULL, -- Tổng số lượng mã tối đa được phép dùng
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Seed dữ liệu mẫu cho concerts, ticket_categories và vouchers
INSERT INTO
    concerts (id, title, description, venue, start_time, status)
VALUES
    (
        1,
        'World Tour Concert 2026',
        'Đêm nhạc hội hoành tráng nhất năm',
        'Sân vận động Quốc gia Mỹ Đình',
        '2026-09-09 20:00:00',
        'PUBLISHED'
    );

INSERT INTO
    ticket_categories (
        concert_id,
        name,
        price,
        total_quantity,
        available_quantity
    )
VALUES
    (1, 'VIP', 2000000.00, 100, 100),
    (1, 'Standard', 800000.00, 500, 500);

INSERT INTO
    vouchers (
        code,
        discount_type,
        discount_value,
        min_order_value,
        max_discount_amount,
        usage_limit,
        start_time,
        end_time
    )
VALUES
    (
        'SUMMER2026',
        'PERCENTAGE',
        20.00,
        300000.00,
        100.00,
        100,
        '2026-06-01 00:00:00',
        '2026-08-31 23:59:59'
    ),
    (
        'WELCOME100K',
        'FIXED_AMOUNT',
        100000.00,
        500000.00,
        NULL,
        200,
        '2026-07-01 00:00:00',
        '2026-12-31 23:59:59'
    ),
    (
        'VIPCUSTOMER',
        'PERCENTAGE',
        25.00,
        1000000.00,
        30.00,
        30,
        '2026-07-15 00:00:00',
        '2026-09-15 23:59:59'
    ),
    (
        'CONCERT50K',
        'FIXED_AMOUNT',
        50000.00,
        200000.00,
        NULL,
        500,
        '2026-07-01 00:00:00',
        '2026-10-01 23:59:59'
    ),
    (
        'EARLYBIRD2026',
        'PERCENTAGE',
        10.00,
        700000.00,
        150.00,
        150,
        '2026-07-01 00:00:00',
        '2026-07-31 23:59:59'
    ),
    (
        'TICKETSALE30',
        'PERCENTAGE',
        30.00,
        1500000.00,
        20.00,
        20,
        '2026-08-01 00:00:00',
        '2026-08-31 23:59:59'
    ),
    (
        'NEWUSER2026',
        'FIXED_AMOUNT',
        75000.00,
        300000.00,
        NULL,
        300,
        '2026-07-01 00:00:00',
        '2026-12-31 23:59:59'
    ),
    (
        'FANCLUB10',
        'PERCENTAGE',
        10.00,
        400000.00,
        80.00,
        80,
        '2026-07-10 00:00:00',
        '2026-09-30 23:59:59'
    ),
    (
        'BIGCONCERT500',
        'FIXED_AMOUNT',
        500000.00,
        3000000.00,
        NULL,
        10,
        '2026-09-01 00:00:00',
        '2026-12-31 23:59:59'
    );