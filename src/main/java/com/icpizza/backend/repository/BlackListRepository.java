package com.icpizza.backend.repository;

import com.icpizza.backend.entity.BlackListCstmr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlackListRepository extends JpaRepository<BlackListCstmr, Long> {
    @Query("""
    select c from BlackListCstmr c where c.customer.telephoneNo =:telephone
""")
    Optional<BlackListCstmr> findByTelephoneNo(@Param("telephone") String telephoneNo);
}
