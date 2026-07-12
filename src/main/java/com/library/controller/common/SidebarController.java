package com.library.controller.common;

import com.library.model.AdminUser;
import com.library.service.AnalyticsService;
import com.library.util.Navigator;
import com.library.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.sql.SQLException;

/**
 * Controller for the shared Sidebar.fxml. Each page calls setActive(...)
 * after including the sidebar so the correct nav item is highlighted.
 */
public class SidebarController {

    @FXML private Button dashboardBtn;
    @FXML private Button booksBtn;
    @FXML private Button membersBtn;
    @FXML private Button onlineLibraryBtn;
    @FXML private Button borrowBtn;
    @FXML private Button returnBtn;
    @FXML private Button reportsBtn;
    @FXML private Button settingsBtn;

    @FXML private Label returnBadge;

    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserEmail;

    public enum NavItem { DASHBOARD, BOOKS, MEMBERS, ONLINE_LIBRARY, BORROW, RETURN, REPORTS, SETTINGS }

    @FXML
    public void initialize() {
        showCurrentUser();

        // "Return Book" badge = how many loans are currently out (i.e. waiting
        // to be returned) — a real, live count, not a fixed placeholder number.
        try {
            long activeLoans = new AnalyticsService().getDashboardMetrics().activeLoans();
            if (activeLoans > 0) {
                returnBadge.setText(activeLoans > 99 ? "99+" : String.valueOf(activeLoans));
                returnBadge.setVisible(true);
                returnBadge.setManaged(true);
            }
        } catch (SQLException e) {
            // Leave the badge hidden if we can't reach the database yet.
        }
    }

    private void showCurrentUser() {
        AdminUser user = Session.getCurrentUser();
        if (user == null) {
            return; // shouldn't happen (sidebar only shows on post-login screens), but don't crash if it does
        }
        String name = (user.getFullName() == null || user.getFullName().isBlank())
                ? user.getUsername() : user.getFullName();
        sidebarUserName.setText(name);
        sidebarUserEmail.setText(user.getUsername());
    }

    /**
     * Highlights the nav item matching the currently displayed page.
     * Call this from the page controller's initialize() method, e.g.:
     * sidebarController.setActive(SidebarController.NavItem.BOOKS);
     */
    public void setActive(NavItem item) {
        clearActive();
        Button target = switch (item) {
            case DASHBOARD -> dashboardBtn;
            case BOOKS -> booksBtn;
            case MEMBERS -> membersBtn;
            case ONLINE_LIBRARY -> onlineLibraryBtn;
            case BORROW -> borrowBtn;
            case RETURN -> returnBtn;
            case REPORTS -> reportsBtn;
            case SETTINGS -> settingsBtn;
        };
        target.getStyleClass().add("sidebar-item-active");
    }

    private void clearActive() {
        for (Button b : new Button[]{dashboardBtn, booksBtn, membersBtn, onlineLibraryBtn, borrowBtn, returnBtn, reportsBtn, settingsBtn}) {
            b.getStyleClass().remove("sidebar-item-active");
        }
    }

    @FXML private void goToDashboard() { Navigator.goTo("/fxml/Dashboard.fxml"); }
    @FXML private void goToBooks()     { Navigator.goTo("/fxml/Books.fxml"); }
    @FXML private void goToMembers()   { Navigator.goTo("/fxml/Members.fxml"); }
    @FXML private void goToOnlineLibrary() { Navigator.goTo("/fxml/OnlineLibrary.fxml"); }
    @FXML private void goToBorrow()    { Navigator.goTo("/fxml/Borrow.fxml"); }
    @FXML private void goToReturn()    { Navigator.goTo("/fxml/Return.fxml"); }
    @FXML private void goToReports()   { Navigator.goTo("/fxml/Reports.fxml"); }
    @FXML private void goToSettings()  { Navigator.goTo("/fxml/Settings.fxml"); }

    @FXML
    private void logout() {
        Session.clear();
        Navigator.goTo("/fxml/Login.fxml");
    }
}
