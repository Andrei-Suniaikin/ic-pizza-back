package com.icpizza.backend.repository;

import com.icpizza.backend.entity.ComboItem;
import com.icpizza.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComboItemRepository extends JpaRepository<ComboItem, Long> {
    List<ComboItem> findAllByOrderItemIdIn(List<Long> orderItemIds);

    @Modifying
    @Query("DELETE FROM ComboItem ci WHERE ci.orderItem.id IN :orderItemIds")
    void deleteByOrderItemIds(@Param("orderItemIds") List<Long> orderItemIds);

    List<ComboItem> findByOrderItemId(Long id);

    List<ComboItem> findByOrderItem(OrderItem orderItem);

}
