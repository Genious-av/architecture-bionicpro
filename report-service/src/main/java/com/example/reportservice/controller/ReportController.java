
package com.example.reportservice.controller;

import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.security.JwtClientIdExtractor;
import com.example.reportservice.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final JwtClientIdExtractor jwtClientIdExtractor;

    public ReportController(
            ReportService reportService,
            JwtClientIdExtractor jwtClientIdExtractor
    ) {
        this.reportService = reportService;
        this.jwtClientIdExtractor = jwtClientIdExtractor;
    }

    @GetMapping
    public List<ReportResponse> getReports(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo
    ) {
        Long clientId = jwtClientIdExtractor.extractClientId(authorization);

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "dateFrom must not be after dateTo"
            );
        }

        return reportService.getReports(clientId, dateFrom, dateTo);
    }
}
