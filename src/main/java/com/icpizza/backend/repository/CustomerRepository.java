package com.icpizza.backend.repository;

import com.icpizza.backend.entity.Customer;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    Optional<Customer> findByTelephoneNo(String telephoneNo);

    @Query("select coalesce(sum(c.amountPaid), 0) from Customer c")
    BigDecimal sumAllAmountPaid();

    @Query("select count(distinct c.telephoneNo) from Customer c")
    long countDistinctTelephoneNo();

    @Query("select coalesce(sum(c.amountOfOrders), 0) from Customer c")
    long sumAllAmountOfOrders();

    @Query("select count(c) from Customer c where c.amountOfOrders > 1")
    long countRepeatCustomers();

    @Query("""
    select c from Customer c where c.id=:userId
""")
    Customer findByUserId(@Param("userId") String userId);
}
