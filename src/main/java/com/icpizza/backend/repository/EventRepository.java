package com.icpizza.backend.repository;

import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("select coalesce(max(e.shiftNo), 0) from Event e where e.branch = :branch")
    Integer findLastShiftNo(@Param("branch") Branch branch);

    @Query(value = """
    SELECT * FROM events
    WHERE branch_id=:branchId
     AND type=:type
     AND shift_no=:shiftNo
    ORDER BY datetime DESC 
    limit 1
""",  nativeQuery = true)
    Optional<Event> findTodaysOpenCashEvent(
            @Param("branchId") UUID branchId,
            @Param("type")String type,
            @Param("shiftNo") Integer shiftNo
    );

    @Query(value = """
    SELECT * FROM events
    WHERE branch_id=:branchId\s
      AND cash_amount IS NULL\s
    ORDER BY datetime DESC, id DESC\s
    LIMIT 1
""",  nativeQuery = true)
    Optional<Event> findLastDefaultEvent(UUID branchId);

    @Query(value = """
    SELECT * FROM events
    WHERE branch_id=:branchId\s
      AND cash_amount IS NOT NULL\s
    ORDER BY datetime DESC, id DESC\s
    LIMIT 1
""",  nativeQuery = true)
    Optional<Event> findLastCashEvent(@Param("branchId") UUID branchId);
}
