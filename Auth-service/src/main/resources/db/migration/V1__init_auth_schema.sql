-- 1. Tạo bảng users cho auth_db
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

-- 2. Tạo Index tối ưu tra cứu theo email
CREATE INDEX idx_users_email ON users(email);

-- 3. Seed dữ liệu mẫu (Mật khẩu đã được mã hóa sẵn bằng BCrypt)
INSERT INTO users (username, email, password_hash, full_name, phone_number, role, is_active)
VALUES
    (
        'operator_admin',
        'admin.concert@gmail.com',
        '$2a$10$6Pepz/kLIn7DMcGn7YMGw.EmxF3z/LjlU/So.5v49Yk8QRQW.8uVm',
        'System Administrator',
        '0987654321',
        'ROLE_OPERATOR',
        TRUE
    ),
    (
        'nguyentheanh',
        'theanh.dev@gmail.com',
        '$2a$10$6Pepz/kLIn7DMcGn7YMGw.EmxF3z/LjlU/So.5v49Yk8QRQW.8uVm',
        'Nguyễn Thế Anh',
        '0123456789',
        'ROLE_CUSTOMER',
        TRUE
    );