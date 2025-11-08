package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p.id FROM Product p WHERE p.isPurchasable = true")
    Set<Long> findIdsByPurchasableTrue();
    List<Product> findAllByIdIn(Collection<Long> ids);
}
