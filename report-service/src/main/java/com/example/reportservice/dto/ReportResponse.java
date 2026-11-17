package com.example.reportservice.dto;

import java.time.LocalDate;

public record ReportResponse(
        Long clientId,
        String deviceNumber,
        LocalDate operationDate,
        Long operationsCount
) {
}
