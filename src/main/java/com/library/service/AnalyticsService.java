package com.library.service;

import com.library.model.dto.DashboardMetrics;
import com.library.model.dto.LoanDetails;
import com.library.repository.AnalyticsRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service providing statistical aggregations for dashboards and reports.
 */
public class AnalyticsService {
    
    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService() {
        this.analyticsRepository = new AnalyticsRepository();
    }

    public AnalyticsService(AnalyticsRepository repository) {
        this.analyticsRepository = repository;
    }

    public DashboardMetrics getDashboardMetrics() throws SQLException {
        return analyticsRepository.getDashboardMetrics();
    }

    public Map<String, Integer> getLoanTrend(int months) throws SQLException {
        if (months <= 0) throw new IllegalArgumentException("Months must be positive");
        return analyticsRepository.getLoanTrend(months);
    }

    public Map<String, Integer> getPopularCategories(int limit) throws SQLException {
        if (limit <= 0) throw new IllegalArgumentException("Limit must be positive");
        return analyticsRepository.getPopularCategories(limit);
    }

    public List<LoanDetails> getRecentLoans(int limit, boolean returnedOnly) throws SQLException {
        if (limit <= 0) throw new IllegalArgumentException("Limit must be positive");
        return analyticsRepository.getRecentLoans(limit, returnedOnly);
    }
}
