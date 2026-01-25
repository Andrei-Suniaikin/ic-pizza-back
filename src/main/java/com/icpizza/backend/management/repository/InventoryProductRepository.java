package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.dto.ProductInfo;
import com.icpizza.backend.management.entity.InventoryProduct;
import com.icpizza.backend.management.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {
    List<InventoryProduct> getByReport(Report report);

    void deleteAllByReport(Report report);

    @Query("""
    SELECT new com.icpizza.backend.management.dto.ProductInfo(
      ii.product.id, 
      cast(
        (COALESCE(ii.kitchenQuantity, :zero) + coalesce(ii.storageQuantity, :zero)) 
      as java.math.BigDecimal),
      ii.totalPrice)
    FROM InventoryProduct ii
    WHERE ii.report.id = :reportId AND ii.product.id IN :productIds
  """)
    List<ProductInfo> loadByReport(@Param("reportId")Long reportId,
                                   @Param("productIds")Collection<Long> productIds,
                                   @Param("zero") BigDecimal zero);
}
