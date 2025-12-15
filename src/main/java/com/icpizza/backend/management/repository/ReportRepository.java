package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("""
    SELECT r FROM Report r WHERE r.branch.branchNumber= :branchNo AND r.type="INVENTORY" ORDER BY r.createdAt DESC
""")
    List<Report> findAllByBranchDesc(@Param("branchNo") Integer branchNo);

    @Query("""
    SELECT r FROM Report r WHERE r.type=:type ORDER BY r.createdAt DESC
""")
    List<Report> findAllByType(@Param("type") ReportType type);

    @Query("""
    SELECT r FROM Report r
    WHERE r.branch.id=:branchId
      AND r.type=:type
      AND lower(r.title) LIKE concat(lower(:prefix),'%')
    ORDER BY r.createdAt DESC
  """)
    List<Report> findByPrefix(UUID branchId, ReportType type, String prefix);

    @Query("""
    SELECT r FROM Report r
    WHERE r.branch.id=:branchId AND r.type=:type AND r.title=:title
  """)
    Optional<Report> findByBranchTypeTitle(UUID branchId, ReportType type, String title);

    default Optional<Report> findSinglePurchaseByPrefix(UUID branchId, String prefix){
        var list = findByPrefix(branchId, ReportType.PURCHASE, prefix);
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    Optional<Report> findTopByTypeOrderByCreatedAtDesc(ReportType type);

}
