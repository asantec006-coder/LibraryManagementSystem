package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.model.AdminUser;
import com.library.model.Book;
import com.library.repository.BookRepository;
import com.library.service.BookService;
import com.library.util.CoverImageLoader;
import com.library.util.Session;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import com.library.service.AnalyticsService;
import com.library.model.dto.DashboardMetrics;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Controller for Dashboard.fxml. Every number and every book shown here is
 * loaded from the real database via AnalyticsService/BookService — no
 * mock/sample data.
 */
public class DashboardController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private LineChart<String, Number> trendChart;
    @FXML private CategoryAxis trendXAxis;
    @FXML private NumberAxis trendYAxis;
    @FXML private VBox categoryBarsBox;
    @FXML private HBox recentBooksBox;

    @FXML private Label totalBooksLabel;
    @FXML private Label activeMembersLabel;
    @FXML private Label borrowedBooksLabel;
    @FXML private Label overdueBooksLabel;

    private AnalyticsService analyticsService;
    private BookService bookService;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.DASHBOARD);

        AdminUser user = Session.getCurrentUser();
        String name = (user == null || user.getFullName() == null || user.getFullName().isBlank())
                ? "Librarian" : user.getFullName();
        topBarController.setTitle("Dashboard", "Welcome back, " + name + "!");

        analyticsService = new AnalyticsService();
        bookService = new BookService(new BookRepository());
        loadDashboardData();
    }

    private void loadDashboardData() {
        try {
            DashboardMetrics metrics = analyticsService.getDashboardMetrics();
            Map<String, Integer> trend = analyticsService.getLoanTrend(6);
            Map<String, Integer> popularCategories = analyticsService.getPopularCategories(5);
            List<Book> recentBooks = bookService.getAllBooks().stream()
                    .sorted(Comparator.comparingInt(Book::getId).reversed())
                    .limit(6)
                    .toList();

            Platform.runLater(() -> {
                updateStats(metrics);
                buildTrendChart(trend);
                buildCategoryBars(popularCategories);
                buildRecentBooks(recentBooks);
            });
        } catch (SQLException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load dashboard data: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            });
            e.printStackTrace();
        }
    }

    private void updateStats(DashboardMetrics metrics) {
        totalBooksLabel.setText(String.format("%,d", metrics.totalBooks()));
        activeMembersLabel.setText(String.format("%,d", metrics.activeMembers()));
        borrowedBooksLabel.setText(String.format("%,d", metrics.activeLoans()));
        overdueBooksLabel.setText(String.format("%,d", metrics.overdueLoans()));
    }

    private void buildTrendChart(Map<String, Integer> trend) {
        trendChart.getData().clear();

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        if (trend.isEmpty()) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>("No Data", 0));
        } else {
            trend.forEach((month, value) -> series.getData().add(new javafx.scene.chart.XYChart.Data<>(month, value)));
        }

        trendChart.getData().add(series);
    }

    private void buildCategoryBars(Map<String, Integer> categories) {
        if (categories.isEmpty()) {
            categories.put("No data", 0);
        }

        final int finalMax = Math.max(categories.values().stream().max(Integer::compareTo).orElse(1), 1);

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
            fill.setPrefWidth(200 * value / (double) finalMax);

            javafx.scene.layout.StackPane barStack = new javafx.scene.layout.StackPane();
            barStack.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            barStack.getChildren().addAll(track, fill);
            HBox.setHgrow(barStack, Priority.ALWAYS);

            HBox row = new HBox(10, nameLabel, barStack);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            categoryBarsBox.getChildren().add(row);
        });
    }

    private void buildRecentBooks(List<Book> books) {
        recentBooksBox.getChildren().clear();
        if (books.isEmpty()) {
            Label empty = new Label("No books in the catalog yet.");
            empty.getStyleClass().add("section-subtitle");
            recentBooksBox.getChildren().add(empty);
            return;
        }

        for (Book book : books) {
            ImageView cover = new ImageView(CoverImageLoader.load(book));
            cover.setFitWidth(70);
            cover.setFitHeight(100);
            cover.setPreserveRatio(false);
            cover.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);");

            Label title = new Label(book.getTitle());
            title.getStyleClass().add("recent-item-title");
            title.setWrapText(true);
            title.setMaxWidth(80);

            VBox item = new VBox(6, cover, title);
            item.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            item.setMaxWidth(80);
            recentBooksBox.getChildren().add(item);
        }
    }
}
