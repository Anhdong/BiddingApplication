package com.uet.BiddingApplication.Controller.SidebarController;

import com.uet.BiddingApplication.Controller.BaseController.BaseSidebarController;
import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.Enum.ViewPath;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class BidderSidebarController extends BaseSidebarController {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BidderSidebarController.class);

    //--FXML--
    @FXML private ToggleButton btnBrowse, btnWatchlist, btnHistory;

    //--HANDLE NAVIGATE--
    @FXML
    private void handleBrowse() {
        MainViewController.getInstance().loadView(ViewPath.BIDDER_BROWSE);
    }

    @FXML
    private void handleWatchlist() {MainViewController.getInstance().loadView(ViewPath.BIDDER_WATCHLIST);}

    @FXML
    private void handleHistory() {
        MainViewController.getInstance().loadView(ViewPath.BIDDER_HISTORY);
    }

}
