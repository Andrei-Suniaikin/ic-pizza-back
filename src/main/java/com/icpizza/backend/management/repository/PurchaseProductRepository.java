package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.PurchaseProduct;
import com.icpizza.backend.management.entity.PurchaseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseProductRepository extends JpaRepository<PurchaseProduct, Long> {
    @Query(" Select p from PurchaseProduct p where p.report.id=:purchaseReportId")
    List<PurchaseProduct> findAllByPurchaseReport(@Param("purchaseReportId") Long id);
}
