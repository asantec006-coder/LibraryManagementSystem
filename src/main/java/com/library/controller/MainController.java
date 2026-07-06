package com.library.controller;

import com.library.repository.BookRepository;
import com.library.repository.MemberRepository;
import com.library.model.Book;
import com.library.model.Member;
import com.library.service.BookService;
import com.library.service.MemberService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Dashboard shell controller — owns only the Loans tab.
 * Books → BooksController, Members → MemberController.
 */
public class MainController {
    private final BookService bookService     = new BookService(new BookRepository());
    private final MemberService memberService = new MemberService(new MemberRepository());

    @FXML private ComboBox<Book>   issueBookComboBox;
    @FXML private ComboBox<Member> issueMemberComboBox;
    @FXML private Label            loanStatusLabel;

    @FXML
    public void initialize() {
        loadLoansTabData();
    }

    private void loadLoansTabData() {
        try {
            List<Book> books = bookService.getAllBooks();
            issueBookComboBox.setItems(FXCollections.observableArrayList(books));

            List<Member> members = memberService.getAllMembers();
            issueMemberComboBox.setItems(FXCollections.observableArrayList(members));
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
        }
    }

    @FXML
    public void handleIssueBook() {
        Book selectedBook     = issueBookComboBox.getSelectionModel().getSelectedItem();
        Member selectedMember = issueMemberComboBox.getSelectionModel().getSelectedItem();
        if (selectedBook == null || selectedMember == null) {
            showAlert("Selection Required", "Please select a book and a member.");
            return;
        }
        // Borrow Module will replace this stub
        loanStatusLabel.setText("Issue not yet implemented — coming in Borrow Module.");
    }

    @FXML
    public void handleReturnBook() {
        // Return Module will replace this stub
        loanStatusLabel.setText("Return not yet implemented — coming in Return Module.");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
