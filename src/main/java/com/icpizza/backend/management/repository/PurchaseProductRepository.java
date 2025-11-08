package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.dto.ProductInfo;
import com.icpizza.backend.management.entity.PurchaseProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PurchaseProductRepository extends JpaRepository<PurchaseProduct, Long> {
    @Query(" Select p from PurchaseProduct p where p.report.id=:purchaseReportId")
    List<PurchaseProduct> findAllByPurchaseReport(@Param("purchaseReportId") Long id);

    @Query("""
    SELECT new com.icpizza.backend.management.dto.ProductInfo(pp.product.id, SUM(pp.quantity), SUM(pp.quantity * pp.finalPrice))
    FROM PurchaseProduct pp
    WHERE pp.report.id = :reportId
      AND pp.product.id IN :productIds
    GROUP BY pp.product.id
  """)
    List<ProductInfo> aggregateForReport(Long reportId, Collection<Long> productIds);
}
