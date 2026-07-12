package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.model.AdminUser;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.dto.DashboardMetrics;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;
import com.library.service.AnalyticsService;
import com.library.service.BookService;
import com.library.service.LoanService;
import com.library.service.MemberService;
import com.library.util.Navigator;
import com.library.util.Session;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Dashboard.fxml (Bookish-style layout). Every number here —
 * including the "Outstanding Fines" card and "Most Borrowed Books" table,
 * which are new — is computed from the real database. No mock/sample data.
 */
public class DashboardController {

    /** Same late-fee policy used on the Borrow and Return screens ($0.50/day overdue). */
    private static final double FINE_PER_DAY_OVERDUE = 0.50;

    private record BorrowedBookRow(String title, String author, long timesBorrowed) {
    }

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private LineChart<String, Number> trendChart;
    @FXML private CategoryAxis trendXAxis;
    @FXML private NumberAxis trendYAxis;
    @FXML private ComboBox<String> rangeCombo;
    @FXML private PieChart categoryPieChart;

    @FXML private Label totalBooksLabel;
    @FXML private Label activeMembersLabel;
    @FXML private Label borrowedBooksLabel;
    @FXML private Label overdueBooksLabel;
    @FXML private Label outstandingFinesLabel;
    @FXML private Label overdueCountLabel;

    @FXML private TableView<BorrowedBookRow> mostBorrowedTable;
    @FXML private TableColumn<BorrowedBookRow, String> mbTitleColumn;
    @FXML private TableColumn<BorrowedBookRow, String> mbAuthorColumn;
    @FXML private TableColumn<BorrowedBookRow, Long> mbLoansColumn;

    private AnalyticsService analyticsService;
    private BookService bookService;
    private LoanService loanService;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.DASHBOARD);

        AdminUser user = Session.getCurrentUser();
        String name = (user == null || user.getFullName() == null || user.getFullName().isBlank())
                ? "Librarian" : user.getFullName();
        topBarController.setTitle("Dashboard", "Welcome back, " + name + "!");

        analyticsService = new AnalyticsService();
        MemberService memberService = new MemberService(new MemberRepository());
        bookService = new BookService(new BookRepository());
        loanService = new LoanService(new LoanRepository(), bookService, memberService);

        mbTitleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().title()));
        mbAuthorColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().author()));
        mbLoansColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().timesBorrowed()));

        rangeCombo.setItems(FXCollections.observableArrayList("Last 6 months", "Last 12 months"));
        rangeCombo.getSelectionModel().select("Last 6 months");
        rangeCombo.valueProperty().addListener((obs, oldVal, newVal) -> reloadTrendChart());

        loadDashboardData();
    }

    private void reloadTrendChart() {
        int months = "Last 12 months".equals(rangeCombo.getValue()) ? 12 : 6;
        try {
            Map<String, Integer> trend = analyticsService.getLoanTrend(months);
            Platform.runLater(() -> buildTrendChart(trend));
        } catch (SQLException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load trend data: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            });
        }
    }

    private void loadDashboardData() {
        try {
            DashboardMetrics metrics = analyticsService.getDashboardMetrics();
            Map<String, Integer> trend = analyticsService.getLoanTrend(6);
            Map<String, Integer> popularCategories = analyticsService.getPopularCategories(5);
            List<Loan> allLoans = loanService.getAllLoans();
            List<Book> allBooks = bookService.getAllBooks();

            Platform.runLater(() -> {
                updateStats(metrics);
                buildTrendChart(trend);
                buildCategoryPieChart(popularCategories);
                buildOutstandingFines(allLoans);
                buildMostBorrowedTable(allLoans, allBooks);
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

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (trend.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
        } else {
            trend.forEach((month, value) -> series.getData().add(new XYChart.Data<>(month, value)));
        }
        trendChart.getData().add(series);
    }

    private void buildCategoryPieChart(Map<String, Integer> categories) {
        if (categories.isEmpty()) {
            categories = Map.of("No data yet", 1);
        }
        List<PieChart.Data> data = categories.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .toList();
        categoryPieChart.setData(FXCollections.observableArrayList(data));
    }

    /**
     * Real, policy-based estimate of what's currently owed: $0.50/day for
     * every day a still-unreturned loan is past its due date. There's no
     * fine-payment ledger in this app, so this is deliberately an
     * "estimated due" figure, not a fabricated "collected" total.
     */
    private void buildOutstandingFines(List<Loan> loans) {
        List<Loan> overdue = loans.stream().filter(l -> !l.isReturned() && l.isOverdue()).toList();
        double total = overdue.stream()
                .mapToDouble(l -> ChronoUnit.DAYS.between(l.getDueDate(), LocalDate.now()) * FINE_PER_DAY_OVERDUE)
                .sum();
        outstandingFinesLabel.setText(String.format("$%.2f", total));
        overdueCountLabel.setText(overdue.size() + (overdue.size() == 1 ? " overdue loan" : " overdue loans"));
    }

    /** Top 5 books by all-time loan count. */
    private void buildMostBorrowedTable(List<Loan> loans, List<Book> books) {
        Map<Integer, Book> booksById = books.stream().collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));

        Map<Integer, Long> loanCountByBook = loans.stream()
                .collect(Collectors.groupingBy(Loan::getBookId, Collectors.counting()));

        List<BorrowedBookRow> rows = loanCountByBook.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Book book = booksById.get(e.getKey());
                    return new BorrowedBookRow(
                            book == null ? "Unknown book" : book.getTitle(),
                            book == null ? "—" : book.getAuthor(),
                            e.getValue());
                })
                .toList();

        mostBorrowedTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onViewOverdue() {
        Navigator.goTo("/fxml/Return.fxml");
    }
}
