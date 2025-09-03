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

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByExternalId(Long externalId);

    @Modifying
    @Query("update Order o set o.paymentType = :paymentType where o.id = :id")
    int updatePayment(@Param("id") Long id, @Param("paymentType") String paymentType);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.status IN :statuses")
    List<Order> findWithCustomerByStatuses(@Param("statuses") List<String> statuses);

    Optional<Order> findById(Long id);


    @Query("""
           select o from Order o
           left join fetch o.customer c
           where o.createdAt between :from and :to
           """)
    List<Order> findWithCustomerBetween(@Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to);

    @Query("""
           select o
           from Order o
           left join fetch o.customer
           where o.status = :status
             and o.createdAt >= :cutoff
           order by o.createdAt desc
           """)
    List<Order> findReadySince(@Param("status") String status,
                               @Param("cutoff") LocalDateTime cutoff);

    /** Клиенты, чья первая покупка в прошлом месяце (кол-во таких клиентов) */
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

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("""
        select coalesce(sum(o.amountPaid), 0)
        from Order o
        where o.createdAt >= :start and o.createdAt <= :end
    """)
    BigDecimal sumAmountPaidBetween(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

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

    @Query("""
           select coalesce(sum(o.amountPaid), 0)
             from Order o
            where (lower(o.paymentType) like 'cash%' or lower(o.paymentType) = 'cash')
              and o.createdAt >= :from and o.createdAt <= :to
           """)
    BigDecimal sumCashBetween(LocalDateTime from, LocalDateTime to);
}
