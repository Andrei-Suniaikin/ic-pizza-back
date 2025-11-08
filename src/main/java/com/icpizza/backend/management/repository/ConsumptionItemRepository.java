package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.ProductConsumptionItem;
import com.icpizza.backend.management.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsumptionItemRepository extends JpaRepository<ProductConsumptionItem, Long> {
    @Modifying
    @Query("DELETE FROM ProductConsumptionItem ci WHERE ci.report.id=:reportId")
    void deleteByReportId(Long reportId);

//    @Param("reportId")

    List<ProductConsumptionItem> findAllByReport(Report report);
}
