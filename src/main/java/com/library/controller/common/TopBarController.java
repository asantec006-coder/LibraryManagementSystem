package com.library.controller.common;

import com.library.model.AdminUser;
import com.library.repository.BookRepository;
import com.library.repository.MemberRepository;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.util.Navigator;
import com.library.util.PendingSearch;
import com.library.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/**
 * Controller for the shared TopBar.fxml. Each page calls setTitle(...)
 * after including the top bar, e.g.:
 * topBarController.setTitle("Books", "Manage your library collection");
 *
 * The search box searches real books and members (pressing Enter), and
 * navigates to whichever page actually has matches, pre-filling that
 * page's own search field via PendingSearch. The user badge on the right
 * shows whoever is actually signed in (via Session), not a fixed name.
 */
public class TopBarController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField searchField;
    @FXML private Label topbarUserInitials;
    @FXML private Label topbarUserName;

    @FXML
    public void initialize() {
        AdminUser user = Session.getCurrentUser();
        if (user == null) {
            return; // shouldn't happen (top bar only shows on post-login screens), but don't crash if it does
        }
        String name = (user.getFullName() == null || user.getFullName().isBlank())
                ? user.getUsername() : user.getFullName();
        topbarUserName.setText(name);
        topbarUserInitials.setText(initialsOf(name));
    }

    private String initialsOf(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "?";
        }
        String initials = String.valueOf(parts[0].charAt(0));
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            initials += parts[parts.length - 1].charAt(0);
        }
        return initials.toUpperCase();
    }

    public void setTitle(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }

    @FXML
    private void goToNotifications() {
        Navigator.goTo("/fxml/Settings.fxml");
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        try {
            BookService bookService = new BookService(new BookRepository());
            if (!bookService.searchBooks(query).isEmpty()) {
                PendingSearch.set(PendingSearch.Target.BOOKS, query);
                Navigator.goTo("/fxml/Books.fxml");
                return;
            }

            MemberService memberService = new MemberService(new MemberRepository());
            if (!memberService.searchMembers(query).isEmpty()) {
                PendingSearch.set(PendingSearch.Target.MEMBERS, query);
                Navigator.goTo("/fxml/Members.fxml");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "No books or members matched \"" + query + "\".", ButtonType.OK);
            alert.setHeaderText("No results");
            alert.showAndWait();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Search failed: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }
}
