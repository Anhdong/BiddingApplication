package com.uet.BiddingApplication.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainViewController {

    @FXML
    private StackPane SidebarSlot;
    @FXML
    private StackPane ContentSlot;

    @FXML
    public void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/fxml/MainLayout/BidderSidebar.fxml"));
            Parent sidebar = loader.load();

            SidebarSlot.getChildren().setAll(sidebar);

        } catch (IOException e){System.err.println("[MainViewController] Không tải đc sidebar" );}
    }

}
