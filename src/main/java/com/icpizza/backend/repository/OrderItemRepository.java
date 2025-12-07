package com.icpizza.backend.repository;

import com.icpizza.backend.dto.TopProductsStat;
import com.icpizza.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByOrderId(Long id);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    List<OrderItem> findByOrderId(Long orderId);

    void deleteAllByOrderId(Long orderId);

    @Query(value = """
    SELECT oi.name as name, count(oi) as amount
    FROM OrderItem oi 
    WHERE  oi.order.createdAt BETWEEN :start AND :end
    GROUP BY oi.name 
    ORDER BY amount DESC
    LIMIT 5
""")
    List<TopProductsStat> findTopProducts(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT oi.id FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<Long> findIdsByOrderId(@Param("orderId") Long orderId);
}
