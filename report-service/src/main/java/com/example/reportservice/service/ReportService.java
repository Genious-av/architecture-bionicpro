
package com.example.reportservice.service;

import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<ReportResponse> getReports(
            Long clientId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return reportRepository.findReports(clientId, dateFrom, dateTo);
    }
}
