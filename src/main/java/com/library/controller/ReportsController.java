package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.application.Platform;

import com.library.service.AnalyticsService;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controller for Reports.fxml. Visual/static prototype with mock chart
 * data. When the Reports Module exists, replace both mock maps with
 * aggregate queries (e.g. BorrowService.getMonthlyActivity(),
 * BookService.getCategoryDistribution()).
 */
public class ReportsController {

    @FXML
    private SidebarController sidebarController;
    @FXML
    private TopBarController topBarController;

    @FXML
    private LineChart<String, Number> activityChart;
    @FXML
    private PieChart categoryPieChart;

    private AnalyticsService analyticsService;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.REPORTS);
        topBarController.setTitle("Reports", "Analytics & insights");

        analyticsService = new AnalyticsService();
        loadReportData();
    }

    private void loadReportData() {
        try {
            Map<String, Integer> trend = analyticsService.getLoanTrend(12);
            Map<String, Integer> categories = analyticsService.getPopularCategories(10);

            Platform.runLater(() -> {
                buildActivityChart(trend);
                buildCategoryPieChart(categories);
            });
        } catch (SQLException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load report data: " + e.getMessage(),
                        ButtonType.OK);
                alert.showAndWait();
            });
            e.printStackTrace();
        }
    }

    private void buildActivityChart(Map<String, Integer> trend) {
        activityChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (trend.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
        } else {
            trend.forEach((month, value) -> series.getData().add(new XYChart.Data<>(month, value)));
        }

        activityChart.getData().add(series);
    }

    private void buildCategoryPieChart(Map<String, Integer> categories) {
        if (categories.isEmpty()) {
            categories.put("No data", 1);
        }

        categoryPieChart.getData().clear();
        categories.forEach((name, value) -> {
            categoryPieChart.getData().add(new PieChart.Data(name, value));
        });
    }

    @FXML
    private void onExport() {
        try {
            java.io.File file = new java.io.File("Library_Report.csv");
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                writer.println("Category,Count");
                Map<String, Integer> categories = analyticsService.getPopularCategories(100);
                for (Map.Entry<String, Integer> entry : categories.entrySet()) {
                    writer.println(entry.getKey() + "," + entry.getValue());
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Report exported to " + file.getAbsolutePath(),
                    ButtonType.OK);
            alert.setHeaderText("Export Successful");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }
}
