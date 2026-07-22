package com.ticket.core.repository;

import com.ticket.core.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByTicketCategoryId(Long ticketCategoryId);

    long countByTicketCategoryId(Long ticketCategoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = :status WHERE s.id IN :ids")
    int updateStatusForIds(@Param("ids") List<Long> ids, @Param("status") String status);

    @Query("SELECT s FROM Seat s WHERE s.id IN :ids")
    List<Seat> findAllByIds(@Param("ids") List<Long> ids);
}
