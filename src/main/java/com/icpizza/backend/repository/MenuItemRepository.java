package com.icpizza.backend.repository;

import com.icpizza.backend.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           update MenuItem m
              set m.available = :enabled
            where upper(m.size) = upper(:size)
           """)
    int updateAvailableBySize(@Param("size") String size,
                              @Param("enabled") boolean enabled);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update MenuItem m set m.available = :enabled where lower(m.name) = lower(:name)")
    int updateAvailableByNameIgnoreCase(@Param("name") String name,
                                            @Param("enabled") boolean enabled);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update MenuItem m set m.available = :enabled where lower(m.category) = lower(:category)")
    int updateAvailableByCategoryIgnoreCase(@Param("category") String brickPizzas,
                                            @Param("enabled")Boolean enabled);

    List<MenuItem> findAllByOrderByIdAsc();

}
