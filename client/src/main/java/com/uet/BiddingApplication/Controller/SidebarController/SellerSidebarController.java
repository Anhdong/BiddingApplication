package com.uet.BiddingApplication.Controller.SidebarController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerSidebarController implements Initializable {
    @FXML private ToggleGroup sidebarGroup; // Inject từ FXML qua @FXML

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
