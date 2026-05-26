package com.uet.BiddingApplication.Controller.BaseController;

import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.AuthResponseDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Main;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.AppExecutor;
import com.uet.BiddingApplication.Util.NotificationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class BaseSidebarController implements Initializable {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseSidebarController.class);
    //--FXML--
    @FXML private ToggleGroup sidebarGroup; // Inject từ FXML qua @FXML
    @FXML protected FontIcon btnSetting;
    @FXML protected ContextMenu settingsMenu;

    //--CALLBACK--
    private final Consumer<ResponsePacket<?>> forceLogoutCallback = this::handleForceLogoutResponse;
    private final Consumer<ResponsePacket<?>> logoutCallback = this::handleLogoutResponse;

    //--INIT--
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Subscribe force logout
        ResponseDispatcher.getInstance().subscribe(ActionType.FORCE_LOGOUT,forceLogoutCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.LOGOUT,logoutCallback);

        // Cannot click off selected button & reload if reclick
        sidebarGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Ép nút cũ phải xanh tiếp nếu user click lại chính nó
            }});

        setupSettingsMenu();
    }

    //--NETWORK REQUEST--
    protected void requestLogout(){
        AppExecutor.execute(()->{
            RequestPacket<Void> request = new RequestPacket<>();
            request.setAction(ActionType.LOGOUT);
            request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
            request.setToken(ClientSession.getInstance().getCurrentToken());

            ServerConnection.getInstance().sendRequest(request);

            //Clear ClientSession
            ClientSession.getInstance().logout();
        });
    }

    //--HANDLE RESPONSE--
    private void handleLogoutResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            ResponseDispatcher.getInstance().unsubscribe(ActionType.LOGOUT,logoutCallback);

            NotificationUtil.showInfo(response.getMessage());
            log.info("[BaseSidebar] {}", response.getMessage());

            handleLogout();
        } else {
            log.error("[BaseSidebar] Không thể logout tài khoản.");
        }
    }

    private void handleForceLogoutResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 403) {
            ResponseDispatcher.getInstance().unsubscribe(ActionType.FORCE_LOGOUT,forceLogoutCallback);

            NotificationUtil.showInfo(response.getMessage());
            log.info("[BaseSidebar] {}", response.getMessage());

            handleLogout();

            ServerConnection.getInstance().startAutoReconnect();
        } else {
            log.error("[BaseSidebar] Không thể force logout tài khoản.");
        }
    }

    //--MAIN METHODS--
    private void setupSettingsMenu(){
        //Create menu
        settingsMenu = new ContextMenu();

        // Create menu items
        //MenuItem itemProfile = new MenuItem("Hồ sơ cá nhân");
        MenuItem itemLogout = new MenuItem("Log out");

        SeparatorMenuItem separator = new SeparatorMenuItem();

        //Add Action on items
        itemLogout.setOnAction(event -> {
            log.info("[BaseSidebar] Gửi yêu cầu đăng xuất");
            requestLogout();
        });

        //Add items
        settingsMenu.getItems().addAll(separator, itemLogout);
    }

    protected void handleLogout(){
        //OnHide current controller
        MainViewController.getInstance().clearCacheOnLogout();

        //Switch to Login
        try {
            log.info("[BaseSidebar] Đăng xuất thành công, đang quay về màn hình đăng nhập...");
            Parent mainRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.LOGIN.getPath())));
            Platform.runLater(()->{
                Scene currentScene = primaryStage.getScene();
                currentScene.setRoot(mainRoot);
            });
        } catch (Exception e) {log.error("[BaseSidebar] Cannot load Login",e);}
    }

    @FXML
    protected void showSettingsMenu(MouseEvent event) {
        // Side.BOTTOM nghĩa là menu sẽ thò ra ở bên dưới cái nút.
        // Số 0, 0 là khoảng cách x dịch đi bao nhiêu (Offset)
        settingsMenu.show(btnSetting, Side.BOTTOM, 0, 0);
    }



}