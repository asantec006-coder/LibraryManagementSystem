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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.service.MemberService;
import com.library.repository.MemberRepository;
import com.library.repository.LoanRepository;
import com.library.repository.BookRepository;
import com.library.service.AnalyticsService;
import com.library.service.BookService;
import com.library.service.LoanService;
import com.library.util.PendingSearch;

/**
 * Controller for Members.fxml. Backed by real data via MemberService and
 * AnalyticsService — no mock/sample data. Table and stat cards reload
 * from the database after every add/edit/delete.
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

    @FXML
    private Label totalMembersLabel;
    @FXML
    private Label activeMembersLabel;
    @FXML
    private Label booksOutLabel;
    @FXML
    private Label newThisMonthLabel;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterCombo;

    private MemberService memberService;
    private AnalyticsService analyticsService;
    private LoanService loanService;

    /** Full, unfiltered member list from the last reload — filtering re-slices this instead of re-querying. */
    private List<Member> allMembers = List.of();
    /** memberId -> number of currently-active (unreturned) loans, computed once per reload. */
    private Map<Integer, Long> activeLoanCountByMember = Map.of();

    private record MemberRow(String id, String name, String email, String phone, String joined,
            int borrowed, String type, String status, String avatarStyleClass, Member originalMember) {
    }

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.MEMBERS);
        topBarController.setTitle("Members", "Library member directory");

        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        memberService = new MemberService(new MemberRepository());
        analyticsService = new AnalyticsService();
        loanService = new LoanService(new LoanRepository(), new BookService(new BookRepository()), memberService);

        filterCombo.setItems(FXCollections.observableArrayList("All", Member.TYPE_STANDARD, Member.TYPE_PREMIUM));
        filterCombo.setValue("All");
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

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

        loadMembersFromDatabase();

        String pendingQuery = PendingSearch.consumeIfFor(PendingSearch.Target.MEMBERS);
        if (pendingQuery != null) {
            searchField.setText(pendingQuery);
        }
    }

    private void loadMembersFromDatabase() {
        try {
            List<Member> realMembers = memberService.getAllMembers();

            Map<Integer, Long> loanCounts;
            try {
                loanCounts = loanService.getAllLoans().stream()
                        .filter(l -> !l.isReturned())
                        .collect(Collectors.groupingBy(Loan::getMemberId, Collectors.counting()));
            } catch (SQLException e) {
                loanCounts = Map.of();
            }

            long activeCount = realMembers.stream().filter(m -> Member.STATUS_ACTIVE.equals(m.getStatus())).count();
            LocalDate now = LocalDate.now();
            long newThisMonth = realMembers.stream()
                    .filter(m -> m.getJoinDate() != null
                            && m.getJoinDate().getYear() == now.getYear()
                            && m.getJoinDate().getMonth() == now.getMonth())
                    .count();

            long booksOut;
            try {
                // Source of truth for "currently out" is the loans table itself,
                // not the (unmaintained) booksBorrowed counter on Member.
                booksOut = analyticsService.getDashboardMetrics().activeLoans();
            } catch (SQLException e) {
                booksOut = 0;
            }

            final Map<Integer, Long> loanCountsFinal = loanCounts;
            final long booksOutFinal = booksOut;

            Platform.runLater(() -> {
                allMembers = realMembers;
                activeLoanCountByMember = loanCountsFinal;
                applyFilter();
                totalMembersLabel.setText(String.valueOf(realMembers.size()));
                activeMembersLabel.setText(String.valueOf(activeCount));
                booksOutLabel.setText(String.valueOf(booksOutFinal));
                newThisMonthLabel.setText(String.valueOf(newThisMonth));
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String type = filterCombo.getValue() == null ? "All" : filterCombo.getValue();

        List<MemberRow> rows = allMembers.stream()
                .filter(m -> keyword.isEmpty()
                        || m.getName().toLowerCase().contains(keyword)
                        || m.getEmail().toLowerCase().contains(keyword)
                        || (m.getPhone() != null && m.getPhone().toLowerCase().contains(keyword))
                        || (m.getStudentId() != null && m.getStudentId().toLowerCase().contains(keyword)))
                .filter(m -> "All".equals(type) || type.equals(m.getMembershipType()))
                .map(this::mapToRow)
                .toList();

        memberTable.setItems(FXCollections.observableArrayList(rows));
    }

    private MemberRow mapToRow(Member m) {
        String[] colors = { "avatar-blue", "avatar-purple", "avatar-green", "avatar-orange", "avatar-red",
                "avatar-cyan", "avatar-indigo", "avatar-teal" };
        String avatar = colors[m.getId() % colors.length];
        int borrowed = activeLoanCountByMember.getOrDefault(m.getId(), 0L).intValue();
        return new MemberRow("M" + String.format("%03d", m.getId()), m.getName(), m.getEmail(),
                m.getPhone() == null ? "" : m.getPhone(),
                m.getJoinDate() != null ? m.getJoinDate().toString() : "", borrowed, m.getMembershipType(),
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

                String studentId = row.originalMember().getStudentId();
                if (studentId != null && !studentId.isBlank()) {
                    Label studentIdLabel = new Label("Student ID: " + studentId);
                    studentIdLabel.getStyleClass().add("member-id");
                    textBox.getChildren().add(studentIdLabel);
                }

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
