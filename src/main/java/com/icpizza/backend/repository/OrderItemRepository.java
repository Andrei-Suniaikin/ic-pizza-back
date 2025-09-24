package com.icpizza.backend.repository;

import com.icpizza.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByOrderId(Long id);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    List<OrderItem> findByOrderId(Long orderId);

    void deleteAllByOrderId(Long orderId);

    @Query("SELECT oi.id FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<Long> findIdsByOrderId(@Param("orderId") Long orderId);;
}
