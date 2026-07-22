package com.ticket.core.repository;

import com.ticket.core.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    // Lấy số lượng tồn kho hiện tại
    @Query("SELECT t.availableQuantity FROM TicketCategory t WHERE t.id = :id")
    Integer getAvailableQuantity(@Param("id") Long id);

    // Trừ kho (Chỉ trừ được nếu số lượng tồn >= số lượng yêu cầu)
    @Modifying
    @Transactional
    @Query("UPDATE TicketCategory t SET t.availableQuantity = t.availableQuantity - :qty WHERE t.id = :id AND t.availableQuantity >= :qty")
    int deductQuantity(@Param("id") Long id, @Param("qty") int qty);

    // Hoàn lại kho (Dùng khi đơn hàng hết hạn)
    @Modifying
    @Transactional
    @Query("UPDATE TicketCategory t SET t.availableQuantity = t.availableQuantity + :qty WHERE t.id = :id")
    void refundQuantity(@Param("id") Long id, @Param("qty") int qty);
}