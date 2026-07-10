package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import com.library.model.Member;
import com.library.service.MemberService;
import com.library.repository.MemberRepository;

/**
 * Controller for Members.fxml. Visual/static prototype using mock data.
 * When the Members Module (MemberRepository/MemberService) is wired,
 * replace loadMockMembers() with memberService.getAllMembers() and adapt
 * MemberRow to wrap the real Member model instead.
 */
public class MembersController {

    @FXML
    private SidebarController sidebarController;
    @FXML
    private TopBarController topBarController;

    @FXML
    private TableView<MemberRow> memberTable;
    @FXML
    private TableColumn<MemberRow, MemberRow> memberColumn;
    @FXML
    private TableColumn<MemberRow, MemberRow> contactColumn;
    @FXML
    private TableColumn<MemberRow, String> joinedColumn;
    @FXML
    private TableColumn<MemberRow, Integer> borrowedColumn;
    @FXML
    private TableColumn<MemberRow, String> typeColumn;
    @FXML
    private TableColumn<MemberRow, String> statusColumn;
    @FXML
    private TableColumn<MemberRow, MemberRow> actionsColumn;

    private MemberService memberService;

    private record MemberRow(String id, String name, String email, String phone, String joined,
            int borrowed, String type, String status, String avatarStyleClass, Member originalMember) {
    }

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.MEMBERS);
        topBarController.setTitle("Members", "Library member directory");

        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        memberService = new MemberService(new MemberRepository());

        loadMembersFromDatabase();

        memberColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        memberColumn.setCellFactory(col -> memberCell());

        contactColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        contactColumn.setCellFactory(col -> contactCell());

        joinedColumn
                .setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().joined()));
        borrowedColumn.setCellValueFactory(
                data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().borrowed()));

        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().type()));
        typeColumn.setCellFactory(col -> badgeCell(type -> "Premium".equals(type) ? "badge-amber" : "badge-blue"));

        statusColumn
                .setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
        statusColumn.setCellFactory(col -> badgeCell(status -> "badge-green"));

        actionsColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        actionsColumn.setCellFactory(col -> actionsCell());
    }

    private void loadMembersFromDatabase() {
        try {
            List<Member> realMembers = memberService.getAllMembers();
            List<MemberRow> rows = realMembers.stream().map(this::mapToRow).toList();
            Platform.runLater(() -> {
                memberTable.setItems(FXCollections.observableArrayList(rows));
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private MemberRow mapToRow(Member m) {
        String[] colors = { "avatar-blue", "avatar-purple", "avatar-green", "avatar-orange", "avatar-red",
                "avatar-cyan", "avatar-indigo", "avatar-teal" };
        String avatar = colors[m.getId() % colors.length];
        return new MemberRow("M" + String.format("%03d", m.getId()), m.getName(), m.getEmail(),
                m.getPhone() == null ? "" : m.getPhone(),
                m.getJoinDate() != null ? m.getJoinDate().toString() : "", m.getBooksBorrowed(), m.getMembershipType(),
                m.getStatus(), avatar, m);
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
                javafx.scene.control.Button editBtn = iconButton("\u270E");
                editBtn.setOnAction(e -> handleEdit(row.originalMember()));

                javafx.scene.control.Button deleteBtn = iconButton("\uD83D\uDDD1");
                deleteBtn.setStyle("-fx-text-fill: #dc2626;");
                deleteBtn.setOnAction(e -> handleDelete(row.originalMember()));

                setGraphic(new HBox(6, editBtn, deleteBtn));
            }
        };
    }

    private javafx.scene.control.Button iconButton(String glyph) {
        javafx.scene.control.Button button = new javafx.scene.control.Button(glyph);
        button.getStyleClass().add("icon-btn");
        return button;
    }

    @FXML
    private void onAdd() {
        showMemberDialog(null);
    }

    private void handleEdit(Member member) {
        showMemberDialog(member);
    }

    private void handleDelete(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete '" + member.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Deletion");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                memberService.deleteMember(member.getId());
                loadMembersFromDatabase();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showMemberDialog(Member member) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MemberDialog.fxml"));
            VBox dialogPane = loader.load();
            MemberDialogController controller = loader.getController();
            controller.setMember(member);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(member == null ? "Add Member" : "Edit Member");
            dialog.getDialogPane().setContent(dialogPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                try {
                    Member updatedMember = controller.getMember();
                    if (member == null) {
                        memberService.addMember(updatedMember);
                    } else {
                        memberService.updateMember(updatedMember);
                    }
                } catch (IllegalArgumentException | SQLException e) {
                    controller.showError(e.getMessage());
                    event.consume();
                }
            });

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    loadMembersFromDatabase();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
