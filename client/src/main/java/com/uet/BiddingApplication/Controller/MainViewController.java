package com.uet.BiddingApplication.Controller;

import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Enum.RoleType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML private StackPane SidebarSlot;
    @FXML private StackPane ContentSlot;

    // View Cache
    private final Map<ViewPath, Node> viewCache = new EnumMap<>(ViewPath.class);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUserInterface(RoleType.SELLER);
        loadView(ViewPath.BIDDER_AUCTION);
    }

    public void setupUserInterface(RoleType role) {
        //Check role
        ViewPath sidebarPath = switch (role) {
            case RoleType.BIDDER -> ViewPath.BIDDER_SIDEBAR;
            case RoleType.SELLER -> ViewPath.SELLER_SIDEBAR;
            case RoleType.ADMIN -> ViewPath.ADMIN_SIDEBAR;
        };

        try { //set Sidebar
            FXMLLoader loader = new FXMLLoader(getClass().getResource(sidebarPath.getPath()));
            Parent sidebar = loader.load();
            SidebarSlot.getChildren().setAll(sidebar);
        } catch (Exception e) {System.err.println("[MainViewController] Thiết lập Sidebar không thành công.");}

    }


    public void loadView(ViewPath target) {
        // computeIfAbsent: Nếu có trong cache thì lấy, chưa có thì mới chạy đoạn code load FXML
        Node view = viewCache.computeIfAbsent(target, pathEnum -> {
            try {
                System.out.println("Đang load mới: " + pathEnum.getPath());
                FXMLLoader loader = new FXMLLoader(getClass().getResource(pathEnum.getPath()));
                return loader.load();
            } catch (IOException e) {
                System.out.println("[MainViewController] Không thể thiết lập giao diện nội dung.");
                return null;
            }
        });
        if (view != null) {
            ContentSlot.getChildren().setAll(view);
        }
    }
}
