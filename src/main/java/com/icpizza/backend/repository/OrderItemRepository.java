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
    SELECT t.name as name, SUM(t.amount) as amount
    FROM (
        SELECT oi.name as name, COUNT(*) as amount
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE o.created_at BETWEEN :start AND :end
            AND oi.category != 'Combo Deals'
        GROUP BY oi.name

        UNION ALL

        SELECT ci.name as name, COUNT(*) as amount
        FROM combo_items ci
        JOIN order_items oi ON ci.order_item_id = oi.id
        JOIN orders o ON oi.order_id = o.id
        WHERE o.created_at BETWEEN :start AND :end
        GROUP BY ci.name
    ) as t
    GROUP BY t.name
    ORDER BY amount DESC
    LIMIT 10
""", nativeQuery = true)
    List<TopProductsStat> findTopProducts(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT oi.id FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<Long> findIdsByOrderId(@Param("orderId") Long orderId);
}
