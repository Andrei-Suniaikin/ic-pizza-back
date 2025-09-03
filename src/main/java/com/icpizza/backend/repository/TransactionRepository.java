package com.icpizza.backend.repository;

import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    public void deleteByOrder(Order order);

    public Boolean existsByOrder(Order order);

    @Query("""
           SELECT COALESCE(SUM(t.amount), 0)
           FROM Transaction t
           WHERE t.type = 'Cash' 
           AND t.dateTime >= :from AND t.dateTime <= :to
           AND t.branch.id = :branchId
           """)
    BigDecimal sumCashBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") UUID branchId
    );
}
