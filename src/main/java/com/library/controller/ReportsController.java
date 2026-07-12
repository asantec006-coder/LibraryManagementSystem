package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;
import com.library.service.AnalyticsService;
import com.library.service.BookService;
import com.library.service.LoanService;
import com.library.service.MemberService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Controller for Reports.fxml. Every number here is computed from the
 * real books/members/loans tables — no mock/sample data.
 */
public class ReportsController {

    private static final double FINE_PER_DAY_OVERDUE = 0.50;

    @FXML
    private SidebarController sidebarController;
    @FXML
    private TopBarController topBarController;

    @FXML
    private LineChart<String, Number> activityChart;
    @FXML
    private PieChart categoryPieChart;

    @FXML private Label avgBooksPerMonthLabel;
    @FXML private Label memberGrowthLabel;
    @FXML private Label memberGrowthSublabel;
    @FXML private Label returnRateLabel;
    @FXML private Label outstandingFinesLabel;

    private AnalyticsService analyticsService;
    private MemberService memberService;
    private LoanService loanService;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.REPORTS);
        topBarController.setTitle("Reports", "Analytics & insights");

        analyticsService = new AnalyticsService();
        memberService = new MemberService(new MemberRepository());
        loanService = new LoanService(new LoanRepository(), new BookService(new BookRepository()), memberService);

        loadReportData();
    }

    private void loadReportData() {
        try {
            Map<String, Integer> trend = analyticsService.getLoanTrend(12);
            Map<String, Integer> categories = analyticsService.getPopularCategories(10);
            List<Member> members = memberService.getAllMembers();
            List<Loan> loans = loanService.getAllLoans();

            Platform.runLater(() -> {
                buildActivityChart(trend);
                buildCategoryPieChart(categories);
                buildStatCards(trend, members, loans);
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

    private void buildStatCards(Map<String, Integer> trend, List<Member> members, List<Loan> loans) {
        // Avg Books/Month: mean of the same 12-month trend shown in the chart above.
        double avgPerMonth = trend.isEmpty() ? 0 : trend.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        avgBooksPerMonthLabel.setText(String.valueOf(Math.round(avgPerMonth)));

        // Member Growth: members who joined this calendar year.
        int currentYear = LocalDate.now().getYear();
        long newMembersThisYear = members.stream()
                .filter(m -> m.getJoinDate() != null && m.getJoinDate().getYear() == currentYear)
                .count();
        memberGrowthLabel.setText("+" + newMembersThisYear);
        memberGrowthSublabel.setText("New members in " + currentYear);

        // Return Rate: of loans that have been returned, what fraction came back on or before the due date.
        List<Loan> returnedLoans = loans.stream().filter(Loan::isReturned).toList();
        if (returnedLoans.isEmpty()) {
            returnRateLabel.setText("—");
        } else {
            long onTime = returnedLoans.stream()
                    .filter(l -> l.getReturnDate() != null && l.getDueDate() != null
                            && !l.getReturnDate().isAfter(l.getDueDate()))
                    .count();
            double rate = 100.0 * onTime / returnedLoans.size();
            returnRateLabel.setText(String.format("%.1f%%", rate));
        }

        // Estimated Fines Due: policy-based estimate ($0.50/day) applied to loans that are
        // currently overdue and not yet returned. There's no fine-payment ledger in this
        // app, so this is explicitly an estimate of what's currently owed, not a "collected" total.
        double outstandingFines = loans.stream()
                .filter(l -> !l.isReturned() && l.isOverdue())
                .mapToDouble(l -> ChronoUnit.DAYS.between(l.getDueDate(), LocalDate.now()) * FINE_PER_DAY_OVERDUE)
                .sum();
        outstandingFinesLabel.setText(String.format("$%.2f", outstandingFines));
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
