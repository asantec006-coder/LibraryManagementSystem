package com.library.controller;

import com.library.model.Member;
import com.library.repository.MemberRepository;
import com.library.service.MemberService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Members module.
 * Handles UI events and delegates all business operations to MemberService.
 */
public class MemberController {

    private final MemberService memberService = new MemberService(new MemberRepository());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, String> nameCol;
    @FXML private TableColumn<Member, String> emailCol;
    @FXML private TableColumn<Member, String> phoneCol;
    @FXML private TableColumn<Member, String> joinDateCol;
    @FXML private TableColumn<Member, Integer> borrowedCol;
    @FXML private TableColumn<Member, String> typeCol;
    @FXML private TableColumn<Member, String> statusCol;
    @FXML private TableColumn<Member, Boolean> eligibleCol;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private DatePicker membershipDatePicker;
    @FXML private Label eligibilityLabel;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadMembers();

        memberTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> populateForm(newVal)
        );
    }

    private void setupTableColumns() {
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        emailCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        phoneCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));

        joinDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getMembershipDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FMT) : "");
        });

        borrowedCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBooksBorrowed()).asObject());
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMembershipType()));
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

        eligibleCol.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isEligibleToBorrow()));
        eligibleCol.setCellFactory(col -> new javafx.scene.control.cell.CheckBoxTableCell<>());
    }

    private void loadMembers() {
        try {
            List<Member> members = memberService.getAllMembers();
            memberTable.setItems(FXCollections.observableArrayList(members));
        } catch (SQLException e) {
            showError("Database Error", "Failed to load members: " + e.getMessage());
        }
    }

    private void populateForm(Member member) {
        if (member == null) {
            handleClearForm();
            return;
        }
        nameField.setText(member.getFullName());
        emailField.setText(member.getEmail());
        phoneField.setText(member.getPhone());
        membershipDatePicker.setValue(member.getMembershipDate());

        if (member.isEligibleToBorrow()) {
            eligibilityLabel.setText("✔ Eligible to borrow");
            eligibilityLabel.getStyleClass().setAll("eligible-label");
        } else {
            eligibilityLabel.setText("✘ Not eligible to borrow");
            eligibilityLabel.getStyleClass().setAll("ineligible-label");
        }
    }

    @FXML
    public void handleSave() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        int id = selected != null ? selected.getId() : 0;

        Member member = new Member();
        member.setId(id);
        member.setFullName(nameField.getText().trim());
        member.setEmail(emailField.getText().trim());
        member.setPhone(phoneField.getText().trim());
        member.setMembershipDate(membershipDatePicker.getValue());
        member.setMembershipType(selected != null ? selected.getMembershipType() : Member.TYPE_STANDARD);
        member.setStatus(selected != null ? selected.getStatus() : Member.STATUS_ACTIVE);
        member.setBooksBorrowed(selected != null ? selected.getBooksBorrowed() : 0);

        try {
            if (id == 0) {
                memberService.addMember(member);
            } else {
                memberService.updateMember(member);
            }
            showInfo("Success", id == 0 ? "Member added successfully." : "Member updated successfully.");
            handleClearForm();
            loadMembers();
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to save member: " + e.getMessage());
        }
    }

    @FXML
    public void handleDelete() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection Required", "Please select a member from the table to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete member \"" + selected.getFullName() + "\"?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                memberService.deleteMember(selected.getId());
                showInfo("Success", "Member deleted successfully.");
                handleClearForm();
                loadMembers();
            } catch (SQLException e) {
                showError("Database Error", "Failed to delete member: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleSearch() {
        try {
            List<Member> results = memberService.searchMembers(searchField.getText());
            memberTable.setItems(FXCollections.observableArrayList(results));
        } catch (SQLException e) {
            showError("Database Error", "Failed to search members: " + e.getMessage());
        }
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        loadMembers();
    }

    @FXML
    public void handleClearForm() {
        memberTable.getSelectionModel().clearSelection();
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        membershipDatePicker.setValue(null);
        eligibilityLabel.setText("");
        eligibilityLabel.getStyleClass().setAll();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
