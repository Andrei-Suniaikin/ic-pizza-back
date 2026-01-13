package com.icpizza.backend.repository;

import com.icpizza.backend.entity.BranchAvailability;
import com.icpizza.backend.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchAvailabilityRepository extends JpaRepository<BranchAvailability, Long> {
    @Query("""
    SELECT ba.item
     from BranchAvailability ba 
     WHERE ba.branch.id =: branchId
     ORDER BY ba.id ASC 
""")
    List<BranchAvailability> findAllByBranchIdByOrderByIdAsc(@Param("branchId") UUID branchId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update BranchAvailability ba set ba.isAvailable = :enabled where lower(ba.item.name) = lower(:name) and ba.branch.id=:branchId")
    int updateAvailableByNameIgnoreCase(@Param("name") String name,
                                        @Param("enabled") boolean enabled,
                                        @Param("branchId") UUID branchId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update BranchAvailability ba set ba.isAvailable = :enabled where lower(ba.item.category) = lower(:category) and ba.branch.id=:branchId")
    int updateAvailableByCategoryIgnoreCase(@Param("category") String brickPizzas,
                                            @Param("enabled")Boolean enabled,
                                            @Param("branchId") UUID branchId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           update BranchAvailability ba
              set ba.isAvailable = :enabled
            where upper(ba.item.size) = upper(:size)
            and ba.branch.id =: branchId
           """)
    int updateAvailableBySize(@Param("size") String size,
                              @Param("enabled") boolean enabled,
                              @Param("branchId") UUID branchId);
}
