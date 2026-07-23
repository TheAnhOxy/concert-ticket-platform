package com.ticket.core.dto.request;

import com.ticket.core.enums.ConcertStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConcertRequest {
    private String title;
    private String description;
    private String venue;
    private LocalDateTime startTime;
    private ConcertStatus status;
}
