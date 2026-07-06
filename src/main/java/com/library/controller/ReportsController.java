package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for Reports.fxml. Visual/static prototype with mock chart
 * data. When the Reports Module exists, replace both mock maps with
 * aggregate queries (e.g. BorrowService.getMonthlyActivity(),
 * BookService.getCategoryDistribution()).
 */
public class ReportsController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private LineChart<String, Number> activityChart;
    @FXML private PieChart categoryPieChart;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.REPORTS);
        topBarController.setTitle("Reports", "Analytics & insights");

        buildActivityChart();
        buildCategoryPieChart();
    }

    private void buildActivityChart() {
        Map<String, Integer> monthly = new LinkedHashMap<>();
        monthly.put("Jan", 560);
        monthly.put("Feb", 640);
        monthly.put("Mar", 720);
        monthly.put("Apr", 610);
        monthly.put("May", 780);
        monthly.put("Jun", 900);
        monthly.put("Jul", 860);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        monthly.forEach((month, value) -> series.getData().add(new XYChart.Data<>(month, value)));
        activityChart.getData().add(series);
    }

    private void buildCategoryPieChart() {
        categoryPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Fiction", 35),
                new PieChart.Data("Science", 22),
                new PieChart.Data("History", 18),
                new PieChart.Data("Tech", 15),
                new PieChart.Data("Arts", 10)
        ));
    }

    @FXML
    private void onExport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Export will generate a real report file once the Reports Module is built.",
                ButtonType.OK);
        alert.setHeaderText("Coming soon");
        alert.showAndWait();
    }
}
