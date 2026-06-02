package com.uet.BiddingApplication.Controller.SidebarController;

import com.uet.BiddingApplication.Controller.BaseController.BaseSidebarController;
import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.Enum.ViewPath;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class AdminSidebarController extends BaseSidebarController {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminSidebarController.class);

    //--FXML--
    @FXML private ToggleButton btnSessions, btnUsers;

    //--HANDLE NAVIGATE--
    @FXML
    private void handleSessions() {MainViewController.getInstance().loadView(ViewPath.ADMIN_SESSIONS);}

    @FXML
    private void handleUsers() {
        MainViewController.getInstance().loadView(ViewPath.ADMIN_USERS);
    }

}
