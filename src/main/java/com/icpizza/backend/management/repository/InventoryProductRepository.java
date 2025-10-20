package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.InventoryProduct;
import com.icpizza.backend.management.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {
    List<InventoryProduct> getByReport(Report report);

    void deleteAllByReport(Report report);
}
