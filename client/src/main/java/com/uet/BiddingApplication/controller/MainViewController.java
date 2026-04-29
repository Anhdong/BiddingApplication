package com.uet.BiddingApplication.controller;

import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Exception.BusinessException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private StackPane SidebarSlot;
    @FXML
    private StackPane ContentSlot;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUserInterface(RoleType.SELLER);
        setContentSlot("/app/fxml/BidderView/AuctionView.fxml");

    }

    public void setupUserInterface(RoleType role) {
        //Check role
        String sidebarPath = null;
        switch (role){
            case RoleType.BIDDER:
                sidebarPath = "/app/fxml/SidebarView/BidderSidebar.fxml";
                break;
            case RoleType.SELLER:
                sidebarPath = "/app/fxml/SidebarView/SellerSidebar.fxml";
                break;
            case RoleType.ADMIN:
                sidebarPath = "/app/fxml/SidebarView/AdminSidebar.fxml";
                break;
            default:
                throw new RuntimeException("Role Invalid");
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(sidebarPath));
            Parent sidebar = loader.load();
            SidebarSlot.getChildren().setAll(sidebar);
        } catch (Exception e) {e.printStackTrace();}

    }

    public void setContentSlot(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            ContentSlot.getChildren().setAll(content);

        } catch (IOException e){System.err.println("[MainViewController] Không tải  được Tab." );}
    }

}
