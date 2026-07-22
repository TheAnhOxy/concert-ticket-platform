//package com.ticket.booking.repository;
//
//import com.ticket.booking.entity.TicketCategory;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//import org.springframework.transaction.annotation.Transactional;
//
//@Repository
//public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
//
//    // Lấy số lượng tồn kho hiện tại
//    @Query("SELECT t.availableQuantity FROM TicketCategory t WHERE t.id = :id")
//    Integer getAvailableQuantity(@Param("id") Long id);
//
//    // Trừ kho với điều kiện số lượng trong kho phải lớn hơn hoặc bằng số lượng mua (Pessimistic Check)
//    @Modifying
//    @Transactional
//    @Query("UPDATE TicketCategory t SET t.availableQuantity = t.availableQuantity - :qty WHERE t.id = :id AND t.availableQuantity >= :qty")
//    int deductQuantity(@Param("id") Long id, @Param("qty") int qty);
//
//    // Hoàn lại kho (dùng cho Giải pháp 3 khi đơn hàng hết hạn)
//    @Modifying
//    @Transactional
//    @Query("UPDATE TicketCategory t SET t.availableQuantity = t.availableQuantity + :qty WHERE t.id = :id")
//    void refundQuantity(@Param("id") Long id, @Param("qty") int qty);
//}