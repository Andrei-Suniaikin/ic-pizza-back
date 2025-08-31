package com.icpizza.backend.repository;

import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("select coalesce(max(e.shiftNo), 0) from Event e where e.branchId = :branchId")
    Integer findLastShiftNo(String branchId);

    Optional<Event> findTopByBranchIdAndTypeAndShiftNoOrderByDatetimeDesc(
            String branchId, EventType type, Integer shiftNo);

    Optional<Event> findFirstByBranchIdOrderByDatetimeDescIdDesc(String branchId);
}
