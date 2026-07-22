package com.ticket.core.dto.response;

import com.ticket.core.enums.ConcertStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConcertResponse {
    private Long id;
    private String title;
    private String description;
    private String venue;
    private LocalDateTime startTime;
    private ConcertStatus status;
    private LocalDateTime createdAt;
}
