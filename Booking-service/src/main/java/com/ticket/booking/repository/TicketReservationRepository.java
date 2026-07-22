package com.ticket.booking.repository;

import com.ticket.booking.entity.TicketReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketReservationRepository extends JpaRepository<TicketReservation, Long> {
}
