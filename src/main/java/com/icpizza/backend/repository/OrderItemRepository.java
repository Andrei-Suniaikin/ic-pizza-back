package com.icpizza.backend.repository;

import com.icpizza.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByOrderId(Long id);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    List<OrderItem> findByOrderId(Long orderId);
}
