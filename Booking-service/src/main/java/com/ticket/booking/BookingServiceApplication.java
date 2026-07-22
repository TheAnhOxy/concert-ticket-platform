package com.ticket.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableFeignClients
@Slf4j
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Tự động kiểm tra và thêm cột seat_ids vào bảng bookings nếu chưa có
                jdbcTemplate.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS seat_ids VARCHAR(255)");
                log.info("[Self-Healing DB] Đồng bộ cột 'seat_ids' cho bảng bookings thành công!");
            } catch (Exception e) {
                log.error("[Self-Healing DB] Lỗi khi tự động nâng cấp cấu trúc bảng bookings: {}", e.getMessage());
            }
        };
    }
}
