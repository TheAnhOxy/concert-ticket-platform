package com.ticket.booking.service;

import com.ticket.booking.dto.request.BookingRequest;
import com.ticket.booking.dto.response.BookingResponse;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request);
}
