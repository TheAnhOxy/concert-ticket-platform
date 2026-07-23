package com.ticket.core.service.impl;

import com.ticket.core.entity.Seat;
import com.ticket.core.repository.SeatRepository;
import com.ticket.core.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Override
    public List<Seat> getSeatsByCategoryId(Long ticketCategoryId) {
        return seatRepository.findByTicketCategoryId(ticketCategoryId);
    }

    @Override
    @Transactional
    public void generateSeatsForCategory(Long ticketCategoryId, int totalQuantity, String categoryName, List<String> seatNumbers) {
        if (totalQuantity <= 0) return;

        if (seatNumbers != null && !seatNumbers.isEmpty()) {
            log.info("Creating {} custom seats for TicketCategory ID: {}", seatNumbers.size(), ticketCategoryId);
            for (String seatNum : seatNumbers) {
                Seat seat = Seat.builder()
                        .ticketCategoryId(ticketCategoryId)
                        .seatNumber(seatNum)
                        .status("AVAILABLE")
                        .build();
                seatRepository.save(seat);
            }
        } else {
            String prefix = categoryName.split(" ")[0].toUpperCase();
            if (prefix.length() > 5) {
                prefix = prefix.substring(0, 5);
            }

            log.info("Generating {} auto seats for TicketCategory ID: {} with prefix: {}", totalQuantity, ticketCategoryId, prefix);
            for (int i = 1; i <= totalQuantity; i++) {
                Seat seat = Seat.builder()
                        .ticketCategoryId(ticketCategoryId)
                        .seatNumber(prefix + "-" + i)
                        .status("AVAILABLE")
                        .build();
                seatRepository.save(seat);
            }
        }
    }

    @Override
    @Transactional
    public void reserveSeats(List<Long> seatIds, Long ticketCategoryId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new RuntimeException("Danh sách ghế không được để trống!");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new RuntimeException("Một số ghế được chọn không tồn tại trên hệ thống!");
        }

        for (Seat seat : seats) {
            log.info("[DB Check] Ghế ID: {}, Số ghế: {}, Trạng thái hiện tại: {}", seat.getId(), seat.getSeatNumber(), seat.getStatus());
            if (!seat.getTicketCategoryId().equals(ticketCategoryId)) {
                throw new RuntimeException("Ghế " + seat.getSeatNumber() + " không thuộc về hạng vé này!");
            }
            if (!"AVAILABLE".equalsIgnoreCase(seat.getStatus())) {
                throw new RuntimeException("Ghế " + seat.getSeatNumber() + " đã được đặt hoặc đang được giữ chỗ!");
            }
            // Thay đổi trạng thái trực tiếp trên thực thể đã tải vào Persistence Context
            seat.setStatus("RESERVED");
        }

        // Lưu lại bằng saveAll để đồng bộ cả bộ nhớ đệm và DB
        seatRepository.saveAll(seats);
        seatRepository.flush();
        log.info("Reserved seats: {} for category: {}", seatIds, ticketCategoryId);
    }

    @Override
    @Transactional
    public void releaseSeats(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        seatRepository.updateStatusForIds(seatIds, "AVAILABLE");
        log.info("Released seats to AVAILABLE: {}", seatIds);
    }

    @Override
    @Transactional
    public void confirmSeats(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        seatRepository.updateStatusForIds(seatIds, "BOOKED");
        log.info("Confirmed seats to BOOKED: {}", seatIds);
    }
}
