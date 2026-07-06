package com.library.controller.common;

import com.library.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the shared Sidebar.fxml. Each page calls setActive(...)
 * after including the sidebar so the correct nav item is highlighted.
 */
public class SidebarController {

    @FXML private Button dashboardBtn;
    @FXML private Button booksBtn;
    @FXML private Button membersBtn;
    @FXML private Button borrowBtn;
    @FXML private Button returnBtn;
    @FXML private Button reportsBtn;
    @FXML private Button settingsBtn;

    public enum NavItem { DASHBOARD, BOOKS, MEMBERS, BORROW, RETURN, REPORTS, SETTINGS }

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
            case BORROW -> borrowBtn;
            case RETURN -> returnBtn;
            case REPORTS -> reportsBtn;
            case SETTINGS -> settingsBtn;
        };
        target.getStyleClass().add("sidebar-item-active");
    }

    private void clearActive() {
        for (Button b : new Button[]{dashboardBtn, booksBtn, membersBtn, borrowBtn, returnBtn, reportsBtn, settingsBtn}) {
            b.getStyleClass().remove("sidebar-item-active");
        }
    }

    @FXML private void goToDashboard() { Navigator.goTo("/fxml/Dashboard.fxml"); }
    @FXML private void goToBooks()     { Navigator.goTo("/fxml/Books.fxml"); }
    @FXML private void goToMembers()   { Navigator.goTo("/fxml/Members.fxml"); }
    @FXML private void goToBorrow()    { Navigator.goTo("/fxml/Borrow.fxml"); }
    @FXML private void goToReturn()    { Navigator.goTo("/fxml/Return.fxml"); }
    @FXML private void goToReports()   { Navigator.goTo("/fxml/Reports.fxml"); }
    @FXML private void goToSettings()  { Navigator.goTo("/fxml/Settings.fxml"); }

    @FXML
    private void logout() {
        Navigator.goTo("/fxml/Login.fxml");
    }
}
