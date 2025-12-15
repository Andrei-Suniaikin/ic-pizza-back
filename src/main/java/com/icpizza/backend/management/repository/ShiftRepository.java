package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.entity.Shift;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    @Query("""
    SELECT COALESCE(SUM(s.totalHours), 0.0) FROM Shift s WHERE s.report=:report
""")
    Double sumTotalByReport(@Param("report") Report report);

    List<Shift> findAllByReport(Report report);
}
