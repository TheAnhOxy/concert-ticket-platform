package com.ticket.core.service;

import com.ticket.core.entity.Seat;

import java.util.List;

public interface SeatService {
    List<Seat> getSeatsByCategoryId(Long ticketCategoryId);

    void generateSeatsForCategory(Long ticketCategoryId, int totalQuantity, String categoryName, List<String> seatNumbers);

    void reserveSeats(List<Long> seatIds, Long ticketCategoryId);

    void releaseSeats(List<Long> seatIds);

    void confirmSeats(List<Long> seatIds);
}
