package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Borrow.fxml. Visual/static prototype: dropdowns are
 * populated with mock names, and "Issue Book" just confirms the action
 * rather than writing a real BorrowTransaction. When the Borrow Module
 * is built, wire this to BorrowService.issueBook(memberId, bookId, ...).
 */
public class BorrowController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private ComboBox<String> memberCombo;
    @FXML private ComboBox<String> bookCombo;
    @FXML private DatePicker issueDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private VBox recentlyIssuedBox;

    private record RecentIssue(String title, String borrower, String issueDate, String dueDate) {
    }

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.BORROW);
        topBarController.setTitle("Borrow Book", "Issue books to members");

        memberCombo.setItems(FXCollections.observableArrayList(
                "Emily Chen (M001)", "James Wilson (M002)", "Sarah Johnson (M003)", "Daniel Martinez (M004)"));
        bookCombo.setItems(FXCollections.observableArrayList(
                "The Pragmatic Programmer", "Atomic Habits", "Clean Code", "1984"));

        issueDatePicker.setValue(LocalDate.now());

        buildRecentlyIssued();
    }

    private void buildRecentlyIssued() {
        List<RecentIssue> recent = List.of(
                new RecentIssue("Design Patterns", "Emily Chen", "Jul 1, 2026", "Jul 15, 2026"),
                new RecentIssue("Clean Architecture", "James Wilson", "Jun 28, 2026", "Jul 12, 2026")
        );

        recentlyIssuedBox.getChildren().clear();
        for (RecentIssue issue : recent) {
            Label title = new Label(issue.title());
            title.getStyleClass().add("recent-item-title");
            Label borrower = new Label(issue.borrower());
            borrower.getStyleClass().add("recent-item-subtitle");
            Label dates = new Label("\uD83D\uDCC5 " + issue.issueDate() + "   \u23F0 Due: " + issue.dueDate());
            dates.getStyleClass().add("recent-item-date");

            VBox textBox = new VBox(2, title, borrower, dates);

            Label activeBadge = new Label("Active");
            activeBadge.getStyleClass().addAll("badge", "badge-green");

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            HBox row = new HBox(10, textBox, spacer, activeBadge);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            recentlyIssuedBox.getChildren().add(row);
        }
    }

    @SuppressWarnings("unused")
    @FXML
    private void onIssue() {
        if (memberCombo.getValue() == null || bookCombo.getValue() == null || dueDatePicker.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Select a member, a book, and a due date before issuing.", ButtonType.OK);
            alert.setHeaderText("Missing information");
            alert.showAndWait();
            return;
        }
        String message = """
                This will create a real BorrowTransaction once the Borrow Module is wired up.

                Member: %s
                Book: %s
                Due: %s""".formatted(memberCombo.getValue(), bookCombo.getValue(), dueDatePicker.getValue());
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText("Book would be issued");
        alert.showAndWait();
    }
}
