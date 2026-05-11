package com.uet.BiddingApplication.Controller.SidebarController;

import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.Enum.ViewPath;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerSidebarController implements Initializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellerSidebarController.class);
    @FXML private ToggleGroup sidebarGroup; // Inject từ FXML qua @FXML
    @FXML private ToggleButton btnItems, btnHistory;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cannot click off selected button & reload if reclick
        sidebarGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Ép nút cũ phải xanh tiếp nếu user click lại chính nó
            }
        });
    }

    @FXML
    private void handleItems() {
        MainViewController.getInstance().loadView(ViewPath.SELLER_ITEMS);
    }

    @FXML
    private void handleHistory() {
        MainViewController.getInstance().loadView(ViewPath.SELLER_HISTORY);
    }

}
