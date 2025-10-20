package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("""
    SELECT r FROM Report r WHERE r.branch.branchNumber= :branchNo ORDER BY r.createdAt DESC
""")
    List<Report> findAllByBranchDesc(@Param("branchNo") Integer branchNo);
}
