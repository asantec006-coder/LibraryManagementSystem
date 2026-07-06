package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for Members.fxml. Visual/static prototype using mock data.
 * When the Members Module (MemberRepository/MemberService) is wired,
 * replace loadMockMembers() with memberService.getAllMembers() and adapt
 * MemberRow to wrap the real Member model instead.
 */
public class MembersController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private TableView<MemberRow> memberTable;
    @FXML private TableColumn<MemberRow, MemberRow> memberColumn;
    @FXML private TableColumn<MemberRow, MemberRow> contactColumn;
    @FXML private TableColumn<MemberRow, String> joinedColumn;
    @FXML private TableColumn<MemberRow, Integer> borrowedColumn;
    @FXML private TableColumn<MemberRow, String> typeColumn;
    @FXML private TableColumn<MemberRow, String> statusColumn;
    @FXML private TableColumn<MemberRow, MemberRow> actionsColumn;

    private record MemberRow(String id, String name, String email, String phone, String joined,
                              int borrowed, String type, String status, String avatarStyleClass) {
    }

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.MEMBERS);
        topBarController.setTitle("Members", "Library member directory");

        // Columns share available width proportionally (based on their
        // prefWidth as a ratio) instead of staying pinned at fixed pixel
        // widths, so the table actually fills the window on any screen size.
        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        ObservableList<MemberRow> members = FXCollections.observableArrayList(loadMockMembers());
        memberTable.setItems(members);

        memberColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        memberColumn.setCellFactory(col -> memberCell());

        contactColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        contactColumn.setCellFactory(col -> contactCell());

        joinedColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().joined()));
        borrowedColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().borrowed()));

        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().type()));
        typeColumn.setCellFactory(col -> badgeCell(type ->
                "Premium".equals(type) ? "badge-amber" : "badge-blue"));

        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
        statusColumn.setCellFactory(col -> badgeCell(status -> "badge-green"));

        actionsColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        actionsColumn.setCellFactory(col -> actionsCell());
    }

    private java.util.List<MemberRow> loadMockMembers() {
        return java.util.List.of(
                new MemberRow("M001", "Emily Chen", "emily.chen@email.com", "+1 555-0101", "Jan 15, 2024", 3, "Premium", "Active", "avatar-blue"),
                new MemberRow("M002", "James Wilson", "james.w@email.com", "+1 555-0102", "Mar 8, 2024", 5, "Standard", "Active", "avatar-purple"),
                new MemberRow("M003", "Sarah Johnson", "sarah.j@email.com", "+1 555-0103", "Feb 22, 2024", 2, "Premium", "Active", "avatar-green"),
                new MemberRow("M004", "Daniel Martinez", "daniel.m@email.com", "+1 555-0104", "Apr 2, 2024", 1, "Standard", "Active", "avatar-orange"),
                new MemberRow("M005", "Lisa Park", "lisa.park@email.com", "+1 555-0105", "May 10, 2024", 4, "Standard", "Active", "avatar-blue"),
                new MemberRow("M006", "Robert Garcia", "robert.g@email.com", "+1 555-0106", "Jun 18, 2024", 0, "Standard", "Active", "avatar-purple")
        );
    }

    private TableCell<MemberRow, MemberRow> memberCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(MemberRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                String initials = java.util.Arrays.stream(row.name().split(" "))
                        .map(part -> part.substring(0, 1))
                        .reduce("", String::concat);

                Label initialsLabel = new Label(initials);
                initialsLabel.getStyleClass().add("member-avatar-text");
                StackPane avatar = new StackPane(initialsLabel);
                avatar.getStyleClass().addAll("member-avatar", row.avatarStyleClass());

                Label name = new Label(row.name());
                name.getStyleClass().add("member-name");
                Label id = new Label(row.id());
                id.getStyleClass().add("member-id");
                VBox textBox = new VBox(name, id);

                setGraphic(new HBox(10, avatar, textBox));
            }
        };
    }

    private TableCell<MemberRow, MemberRow> contactCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(MemberRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Label email = new Label("\u2709 " + row.email());
                email.getStyleClass().add("member-contact-line");
                Label phone = new Label("\u260E " + row.phone());
                phone.getStyleClass().add("member-contact-line");
                setGraphic(new VBox(2, email, phone));
            }
        };
    }

    private TableCell<MemberRow, String> badgeCell(java.util.function.Function<String, String> styleClassResolver) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(value);
                badge.getStyleClass().addAll("badge", styleClassResolver.apply(value));
                setGraphic(badge);
                setAlignment(Pos.CENTER_LEFT);
            }
        };
    }

    private TableCell<MemberRow, MemberRow> actionsCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(MemberRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                javafx.scene.control.Button viewBtn = iconButton("\uD83D\uDC64");
                javafx.scene.control.Button editBtn = iconButton("\u270E");
                javafx.scene.control.Button deleteBtn = iconButton("\uD83D\uDDD1");
                setGraphic(new HBox(6, viewBtn, editBtn, deleteBtn));
            }
        };
    }

    private javafx.scene.control.Button iconButton(String glyph) {
        javafx.scene.control.Button button = new javafx.scene.control.Button(glyph);
        button.getStyleClass().add("icon-btn");
        return button;
    }

    @SuppressWarnings("unused")
    @FXML
    private void onAdd() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "The Add Member form will be wired up when the Members Module is connected to real data.",
                ButtonType.OK);
        alert.setHeaderText("Coming soon");
        alert.showAndWait();
    }
}
