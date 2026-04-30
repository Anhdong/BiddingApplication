package com.uet.BiddingApplication.Controller;

import com.uet.BiddingApplication.Controller.Enum.ViewPath;
import com.uet.BiddingApplication.Enum.RoleType;
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
        setContentSlot(ViewPath.BIDDER_AUCTION);

    }

    public void setupUserInterface(RoleType role) {
        //Check role
        ViewPath sidebarPath = null;
        switch (role){
            case RoleType.BIDDER:
                sidebarPath = ViewPath.BIDDER_SIDEBAR;
                break;
            case RoleType.SELLER:
                sidebarPath = ViewPath.SELLER_SIDEBAR;
                break;
            case RoleType.ADMIN:
                sidebarPath = ViewPath.ADMIN_SIDEBAR;
                break;
            default:
                throw new RuntimeException("Role Invalid");
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(sidebarPath.getPath()));
            Parent sidebar = loader.load();
            SidebarSlot.getChildren().setAll(sidebar);
        } catch (Exception e) {e.printStackTrace();}

    }

    public void setContentSlot(ViewPath fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath.getPath()));
            Parent content = loader.load();

            ContentSlot.getChildren().setAll(content);

        } catch (IOException e){System.err.println("[MainViewController] Không tải  được Tab." );}
    }

}
