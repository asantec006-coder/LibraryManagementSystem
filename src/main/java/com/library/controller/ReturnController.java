package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller for Return.fxml. Visual/static prototype with mock data.
 * When the Return Module is built, replace both mock lists with real
 * calls into BorrowService (active loans, and recently completed returns).
 */
public class ReturnController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private VBox activeBorrowingsBox;
    @FXML private VBox recentReturnsBox;
    @FXML private Label activeCountLabel;

    private record ActiveBorrow(String title, String borrower, String recordId, String dueDate) {
    }

    private record RecentReturn(String title, String borrower, String date, String condition, String fineLabel, boolean hasFine) {
    }

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.RETURN);
        topBarController.setTitle("Return Book", "Process book returns");

        buildActiveBorrowings();
        buildRecentReturns();
    }

    private void buildActiveBorrowings() {
        List<ActiveBorrow> active = List.of(
                new ActiveBorrow("Thinking, Fast and Slow", "Lisa Park", "BR-038", "Jul 4, 2026"),
                new ActiveBorrow("Zero to One", "Robert Garcia", "BR-039", "Jul 6, 2026"),
                new ActiveBorrow("The Lean Startup", "Sarah Johnson", "BR-040", "Jul 9, 2026"),
                new ActiveBorrow("Clean Architecture", "James Wilson", "BR-041", "Jul 12, 2026")
        );

        activeCountLabel.setText(active.size() + " active borrowings");
        activeBorrowingsBox.getChildren().clear();

        for (ActiveBorrow item : active) {
            Label icon = new Label("\uD83D\uDCD8");
            Label title = new Label(item.title());
            title.getStyleClass().add("active-borrow-title");
            Label subtitle = new Label(item.borrower() + " \u00B7 " + item.recordId());
            subtitle.getStyleClass().add("active-borrow-subtitle");
            VBox textBox = new VBox(2, title, subtitle);

            Label due = new Label("Due: " + item.dueDate());
            due.getStyleClass().add("active-borrow-due");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, icon, textBox, spacer, due);
            row.setAlignment(Pos.CENTER_LEFT);
            activeBorrowingsBox.getChildren().add(row);
        }
    }

    private void buildRecentReturns() {
        List<RecentReturn> returns = List.of(
                new RecentReturn("1984", "Emma Davis", "Jul 3, 2026", "Good", "No Fine", false),
                new RecentReturn("Atomic Habits", "Daniel Martinez", "Jul 2, 2026", "Good", "Fine $2.5", true)
        );

        recentReturnsBox.getChildren().clear();
        for (RecentReturn item : returns) {
            Label title = new Label(item.title());
            title.getStyleClass().add("return-item-title");
            Label subtitle = new Label(item.borrower() + " \u00B7 " + item.date());
            subtitle.getStyleClass().add("return-item-subtitle");
            Label condition = new Label("Condition: " + item.condition());
            condition.getStyleClass().add("return-item-subtitle");
            VBox textBox = new VBox(2, title, subtitle, condition);

            Label badge = new Label(item.fineLabel());
            badge.getStyleClass().addAll("badge", item.hasFine() ? "badge-amber" : "badge-green");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, textBox, spacer, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            recentReturnsBox.getChildren().add(row);
        }
    }
}
