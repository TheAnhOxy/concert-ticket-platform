package com.ticket.core.repository;

import com.ticket.core.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usageLimit = v.usageLimit - 1 " +
            "WHERE v.code = :code " +
            "AND v.usageLimit > 0 " +
            "AND v.startTime <= :now " +
            "AND v.endTime >= :now")
    int decreaseVoucherUsage(@Param("code") String code, @Param("now") LocalDateTime now);
}