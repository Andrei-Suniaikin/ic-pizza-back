package com.icpizza.backend.management.repository;

import com.icpizza.backend.management.entity.PurchaseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseReport, Long> {
}
