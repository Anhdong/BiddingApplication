package com.uet.BiddingApplication.Controller.SidebarController;

import com.uet.BiddingApplication.Controller.BaseController.BaseSidebarController;
import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.Enum.ViewPath;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class SellerSidebarController extends BaseSidebarController {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellerSidebarController.class);

    //--FXML
    @FXML private ToggleButton btnItems, btnHistory;

    //--HANDLE NAVIGATE--
    @FXML
    private void handleItems() {MainViewController.getInstance().loadView(ViewPath.SELLER_ITEMS);}

    @FXML
    private void handleHistory() {
        MainViewController.getInstance().loadView(ViewPath.SELLER_HISTORY);
    }

}
