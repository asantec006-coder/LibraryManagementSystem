package com.library.model.dto;

/**
 * Data Transfer Object containing aggregated metrics for the dashboard.
 */
public record DashboardMetrics(
    int totalBooks,
    int availableBooks,
    long activeMembers,
    long activeLoans,
    long overdueLoans
) {}
