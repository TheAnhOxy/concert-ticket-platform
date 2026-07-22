package com.ticket.booking.repository;

import com.ticket.booking.entity.Booking;
import com.ticket.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByUserId(Long userId);

    // Giả định Entity BookingItem chứa quantity, hoặc Booking có field tổng số lượng vé.
    //  query qua bảng trung gian (hoặc thay đổi cho khớp với Entit):
    @Query("SELECT COALESCE(SUM(bi.quantity), 0) FROM Booking b JOIN b.bookingItems bi " +
            "WHERE b.userId = :userId " +
            "AND b.concertId = :concertId " +
            "AND b.status IN :statuses")
    int countTotalTicketsByUserAndConcert(
            @Param("userId") Long userId,
            @Param("concertId") Long concertId,
            @Param("statuses") List<BookingStatus> statuses
    );

        @Query(value = "SELECT COUNT(*) > 0 FROM bookings b WHERE b.user_id = :userId AND b.status IN :statuses AND b.promotion_snapshot->>'voucherCode' = :voucherCode", nativeQuery = true)
    boolean existsByUserIdAndVoucherCodeAndStatusIn(
            @Param("userId") Long userId,
            @Param("voucherCode") String voucherCode,
            @Param("statuses") List<String> statuses
    );
}
