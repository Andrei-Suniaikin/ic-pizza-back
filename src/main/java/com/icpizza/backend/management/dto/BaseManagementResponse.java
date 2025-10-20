package com.icpizza.backend.management.dto;

import com.icpizza.backend.management.enums.ReportType;

public record BaseManagementResponse(
        Long id,
         ReportType type,
         String title,
         String createdAt,
         Integer branchNo,
         String userName
) {}
