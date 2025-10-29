package com.icpizza.backend.management.dto;

import com.icpizza.backend.management.enums.ReportType;

import java.math.BigDecimal;

public record BaseManagementResponse(
        Long id,
         ReportType type,
         String title,
         String createdAt,
         Integer branchNo,
         String userName,
        BigDecimal finalPrice
) {}
