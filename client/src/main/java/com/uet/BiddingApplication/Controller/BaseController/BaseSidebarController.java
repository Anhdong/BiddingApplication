package com.uet.BiddingApplication.Controller.BaseController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.ResourceBundle;

public class BaseSidebarController implements Initializable {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseSidebarController.class);
    //--FXML--
    @FXML private ToggleGroup sidebarGroup; // Inject từ FXML qua @FXML

    //--INIT--
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cannot click off selected button & reload if reclick
        sidebarGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Ép nút cũ phải xanh tiếp nếu user click lại chính nó
            }
        });
    }

}
