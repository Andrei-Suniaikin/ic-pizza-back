package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.dto.ProductInfo;
import com.icpizza.backend.management.entity.InventoryProduct;
import com.icpizza.backend.management.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {
    List<InventoryProduct> getByReport(Report report);

    void deleteAllByReport(Report report);

    @Query("""
    SELECT new com.icpizza.backend.management.dto.ProductInfo(ii.product.id, ii.quantity, ii.totalPrice)
    FROM InventoryProduct ii
    WHERE ii.report.id = :reportId AND ii.product.id IN :productIds
  """)
    List<ProductInfo> loadByReport(Long reportId, Collection<Long> productIds);
}
