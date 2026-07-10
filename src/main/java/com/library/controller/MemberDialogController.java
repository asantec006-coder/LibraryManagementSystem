package com.library.controller;

import com.library.model.Member;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;

public class MemberDialogController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label errorLabel;
    
    private Member existingMember;

    @FXML
    public void initialize() {
        typeComboBox.getSelectionModel().select("Standard");
        statusComboBox.getSelectionModel().select("Active");
    }

    public void setMember(Member member) {
        this.existingMember = member;
        if (member == null) return;
        
        nameField.setText(member.getName());
        emailField.setText(member.getEmail());
        phoneField.setText(member.getPhone());
        typeComboBox.getSelectionModel().select(member.getMembershipType());
        statusComboBox.getSelectionModel().select(member.getStatus());
    }
    
    public Member getMember() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String type = typeComboBox.getValue();
        String status = statusComboBox.getValue();
        
        if (name.isEmpty() || email.isEmpty()) {
            throw new IllegalArgumentException("Name and email are required.");
        }

        int id = existingMember != null ? existingMember.getId() : 0;
        LocalDate joinDate = existingMember != null ? existingMember.getJoinDate() : LocalDate.now();
        int borrowed = existingMember != null ? existingMember.getBooksBorrowed() : 0;

        return new Member(id, name, email, phone, joinDate, borrowed, type, status);
    }
    
    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
