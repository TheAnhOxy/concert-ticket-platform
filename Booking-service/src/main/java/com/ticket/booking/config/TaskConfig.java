package com.ticket.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class TaskConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Cho phép chạy song song 5 luồng hủy đơn
        scheduler.setThreadNamePrefix("BookingTimeout-");

        // Rất quan trọng: Báo cho luồng biết phải kết thúc êm đẹp khi tắt app
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(15);

        scheduler.initialize();
        return scheduler;
    }
}