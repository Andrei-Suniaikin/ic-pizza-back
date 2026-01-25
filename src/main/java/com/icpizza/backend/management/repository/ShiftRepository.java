package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.dto.shift.ReportHoursDTO;
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
    SELECT new com.icpizza.backend.management.dto.shift.ReportHoursDTO(
        COALESCE(SUM(s.cookTotalHours), 0.0),
        COALESCE(SUM(s.managerTotalHours), 0.0)
    )
     FROM Shift s WHERE s.report=:report
""")
    ReportHoursDTO sumTotalByReport(@Param("report") Report report);

    List<Shift> findAllByReport(Report report);
}
