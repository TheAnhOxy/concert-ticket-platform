package com.ticket.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@Slf4j
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Tự động kiểm tra và thêm cột status vào ticket_categories nếu chưa có
                jdbcTemplate.execute("ALTER TABLE ticket_categories ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE'");
                log.info("[Self-Healing DB] Đồng bộ cột 'status' cho bảng ticket_categories thành công!");
            } catch (Exception e) {
                log.error("[Self-Healing DB] Lỗi đồng bộ ticket_categories: {}", e.getMessage());
            }

            try {
                // Tự động kiểm tra và thêm cột status vào vouchers nếu chưa có
                jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE'");
                log.info("[Self-Healing DB] Đồng bộ cột 'status' cho bảng vouchers thành công!");
            } catch (Exception e) {
                log.error("[Self-Healing DB] Lỗi đồng bộ vouchers: {}", e.getMessage());
            }

            try {
                // Tạo bảng seats nếu chưa tồn tại
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS seats (" +
                        "id SERIAL PRIMARY KEY, " +
                        "ticket_category_id INT REFERENCES ticket_categories(id) ON DELETE CASCADE, " +
                        "seat_number VARCHAR(20) NOT NULL, " +
                        "status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', " +
                        "UNIQUE (ticket_category_id, seat_number)" +
                        ")");
                log.info("[Self-Healing DB] Tạo bảng seats thành công (nếu chưa có)!");
            } catch (Exception e) {
                log.error("[Self-Healing DB] Lỗi đồng bộ bảng seats: {}", e.getMessage());
            }
        };
    }
}
