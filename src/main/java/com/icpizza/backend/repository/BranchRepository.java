package com.icpizza.backend.repository;

import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.enums.WorkLoadLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Branch findByBranchNumber(int i);

    Branch findByExternalId(String s);

    @Query("select b from Branch b where b.id <> :branchId")
    List<Branch> findAllExcludeBranches(@Param("branchId") UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select b from Branch b where b.id=:id")
    Optional<Branch> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT coalesce(b.branchBalance, 0) From Branch b where b.id=:id")
    BigDecimal getBranchBalanceById(@Param("id") UUID branchId);
}
