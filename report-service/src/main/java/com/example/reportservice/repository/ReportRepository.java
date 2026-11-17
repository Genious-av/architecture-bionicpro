
package com.example.reportservice.repository;

import com.example.reportservice.dto.ReportResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;import java.util.List;

@Repository
public class ReportRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ReportRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<ReportResponse> findReports(
            Long clientId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {


        StringBuilder sql = new StringBuilder("""
                SELECT
                    client_id,
                    device_number,
                    operation_date,
                    operations_count
                FROM airflow.client_daily_operations
                WHERE client_id = :clientId
                """);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("clientId", clientId);

        if (dateFrom != null) {
            sql.append(" AND operation_date >= toDate(:dateFrom)");
            parameters.addValue("dateFrom", dateFrom.toString());
        }

        if (dateTo != null) {
            sql.append(" AND operation_date <= toDate(:dateTo)");
            parameters.addValue("dateTo", dateTo.toString());
        }

        sql.append(" ORDER BY operation_date");
        try {
            return namedParameterJdbcTemplate.query(
                    sql.toString(),
                    parameters,
                    (rs, rowNum) -> new ReportResponse(
                            rs.getLong("client_id"),
                            rs.getString("device_number"),
                            rs.getDate("operation_date").toLocalDate(),
                            rs.getLong("operations_count")
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }
}
