package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for Dashboard.fxml. Visual/static prototype: all numbers
 * below are mock data matching the Figma design. When the Reports/Borrow
 * services exist, replace the mock maps with real service calls.
 */
public class DashboardController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private LineChart<String, Number> trendChart;
    @FXML private CategoryAxis trendXAxis;
    @FXML private NumberAxis trendYAxis;
    @FXML private VBox categoryBarsBox;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.DASHBOARD);
        topBarController.setTitle("Dashboard", "Welcome back, Admin Librarian!");

        buildTrendChart();
        buildCategoryBars();
    }

    private void buildTrendChart() {
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
        trendChart.getData().add(series);
    }

    private void buildCategoryBars() {
        Map<String, Integer> categories = new LinkedHashMap<>();
        categories.put("Fiction", 600);
        categories.put("Science", 480);
        categories.put("History", 420);
        categories.put("Tech", 340);
        categories.put("Arts", 220);

        int max = categories.values().stream().max(Integer::compareTo).orElse(1);

        categoryBarsBox.getChildren().clear();
        categories.forEach((name, value) -> {
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("category-bar-label");

            Region track = new Region();
            track.getStyleClass().add("category-bar-track");
            track.setPrefWidth(200);
            HBox.setHgrow(track, Priority.ALWAYS);

            Region fill = new Region();
            fill.getStyleClass().add("category-bar-fill");
            fill.setPrefWidth(200 * value / (double) max);

            javafx.scene.layout.StackPane barStack = new javafx.scene.layout.StackPane();
            barStack.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            barStack.getChildren().addAll(track, fill);
            HBox.setHgrow(barStack, Priority.ALWAYS);

            HBox row = new HBox(10, nameLabel, barStack);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            categoryBarsBox.getChildren().add(row);
        });
    }
}
