package com.icpizza.backend.repository;

import com.icpizza.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByExternalId(Long externalId);

    Optional<Order> findById(Long id);

    @Query("""
  SELECT o FROM Order o
  WHERE o.status != "Picked Up"
  ORDER BY o.createdAt DESC
""")
    List<Order> findActiveOrders();

    @Query("""
       select o
       from Order o
       left join fetch o.customer
       where o.status = "Picked Up"
         and o.createdAt >= :cutoff
       order by o.createdAt desc
       """)
    List<Order> findReadySince(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT o.telephone_no
            FROM orders o
            GROUP BY o.telephone_no
            HAVING MIN(o.created_at) >= :prevStart AND MIN(o.created_at) < :currStart
        ) t
        """, nativeQuery = true)
    Long countFirstTimeCustomersInPrevMonth(@Param("prevStart") Timestamp prevStart,
                                            @Param("currStart") Timestamp currStart);

    /** Сколько из них повторили покупку до certain_date (включительно) */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT o.telephone_no
            FROM orders o
            JOIN (
                SELECT telephone_no, MIN(created_at) AS first_order
                FROM orders
                GROUP BY telephone_no
                HAVING MIN(created_at) >= :prevStart AND MIN(created_at) < :currStart
            ) f ON o.telephone_no = f.telephone_no
            WHERE o.created_at > f.first_order
              AND o.created_at <= :currEnd
        ) retained
        """, nativeQuery = true)
    Long countRetained(@Param("prevStart") Timestamp prevStart,
                       @Param("currStart") Timestamp currStart,
                       @Param("currEnd") Timestamp currEnd);

    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT o.telephone_no
            FROM orders o
            WHERE o.telephone_no IS NOT NULL AND o.telephone_no <> ''
            GROUP BY o.telephone_no
            HAVING MIN(o.created_at) >= :start AND MIN(o.created_at) <= :end
        ) t
    """, nativeQuery = true)
    long countNewUniqueCustomersInWindow(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT COUNT(DISTINCT o.telephone_no)
        FROM orders o
        WHERE o.telephone_no IS NOT NULL AND o.telephone_no <> ''
          AND o.created_at >= :start AND o.created_at <= :end
    """, nativeQuery = true)
    long countUniqueCustomersInWindow(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("SELECT SUM(o.amountPaid) FROM Order o")
    BigDecimal sumAllAmountPaidAllTime();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.type = :type")
    long countByCreatedAtBetweenAndOrderType(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("type") String type);

    @Query("SELECT SUM(o.amountPaid) FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.type = :type")
    BigDecimal sumAmountPaidBetweenAndOrderType(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("type") String type);

    @Query("SELECT o.id FROM Order o WHERE o.branch.branchNumber = :branchNumber AND o.status = 'Kitchen Phase'")
    List<Long> findActiveOrderIdsByBranch(@Param("branchNumber") int branchNumber);

    @Query(value = """
WITH anchor AS (
  SELECT date_trunc('day', now() - interval '2 hours')::date AS biz_today
),
dough_data AS (
  SELECT
    CASE
      WHEN ((i.size = 'S')
         OR (i.size = 'M' AND i.is_thin_dough = TRUE)
         OR (i.name = 'Pizza Combo' AND ((i.size = 'S') OR (i.size = 'M' AND i.is_thin_dough = TRUE)))
      ) THEN 'S Dough'
      WHEN ((i.size = 'M')
         OR (i.size = 'L' AND i.is_thin_dough = TRUE)
         OR (i.name = 'Pizza Combo' AND ((i.size = 'M') OR (i.size = 'L' AND i.is_thin_dough = TRUE)))
      ) THEN 'M Dough'
      WHEN ((i.size = 'L' AND i.is_thin_dough = FALSE)
         OR (i.name = 'Pizza Combo' AND (i.size = 'L' AND i.is_thin_dough = FALSE))
      ) THEN 'L Dough'
      WHEN (i.category = 'Brick Pizzas' OR i.name = 'Detroit Combo') THEN 'Brick Dough'
      ELSE 'Other'
    END AS dough_type,
    (o.created_at - interval '2 hours')::date AS shift_date
  FROM public.order_items i
  JOIN public.orders o ON o.id = i.order_id
  WHERE i.category IN ('Pizzas','Combo Deals','Brick Pizzas')
),
bounds AS (
  SELECT (biz_today - interval '7 days')::date AS from_day,
         (biz_today - interval '1 day')::date AS to_day
  FROM anchor
)
SELECT
  dd.dough_type AS doughType,
  dd.shift_date AS shiftDate,
  COUNT(*)      AS qty
FROM dough_data dd
JOIN bounds b ON TRUE
WHERE dd.shift_date BETWEEN b.from_day AND b.to_day
GROUP BY dd.dough_type, dd.shift_date
ORDER BY dd.dough_type, dd.shift_date
""", nativeQuery = true)
    List<Object[]> getRawDoughUsage();


}