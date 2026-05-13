package com.uet.BiddingApplication.Controller.AuthController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuthResponseDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.AppExecutor;
import com.uet.BiddingApplication.Util.NotificationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class LoginController implements Initializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // Định nghĩa callback thành một biến để có thể unsubscribe chính xác
    private final Consumer<ResponsePacket<?>> loginCallback = this::handleLoginResponse;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Đăng ký lắng nghe sự kiện trả về cho luồng Đăng nhập
        ResponseDispatcher.getInstance().subscribe(ActionType.LOGIN, loginCallback);
    }

    //Main func
    @FXML
    private void handleLogin(){
        //Check điều kiện
        if(!validateInput()) return;

        // Lấy thông tin
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        //Bọc vô DTO
        AppExecutor.execute(()->{
            AuthRequestDTO authDTO = new AuthRequestDTO();
            authDTO.setUsername(username);
            authDTO.setPassword(password);

            RequestPacket<AuthRequestDTO> request = new RequestPacket<>();
            request.setAction(ActionType.LOGIN);
            request.setPayload(authDTO);

            //Gửi request
            ServerConnection.getInstance().sendRequest(request);
        });

    }

    private boolean validateInput(){
        if(txtUsername.getText().isEmpty()){
            NotificationUtil.showError("Username cannot be empty.");
            return false;
        }
        if(txtPassword.getText().isEmpty()){
            NotificationUtil.showError("Password cannot be empty.");
            return false;
        }

        return true;}

    private void handleLoginResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) { // Thành công
            AuthResponseDTO authData = (AuthResponseDTO) response.getPayload();

            // Cập nhật phiên đăng nhập cục bộ
            ClientSession.getInstance().updateLocalSession(authData.getUserProfile(), authData.getToken());
            log.info("Đăng nhập thành công! Xin chào {}", authData.getUserProfile().getUsername());

            //Chuyen ve Main
            switchToMain();
        } else {
            NotificationUtil.showError(response.getMessage());
        }
    }

    public void switchToRegister() {
        //Unsubcribe khi chuyển
        ResponseDispatcher.getInstance().unsubscribe(ActionType.LOGIN, loginCallback);

        try {
            Parent registerRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.REGISTER.getPath())));
            Platform.runLater(()->{
                Scene currentScene = primaryStage.getScene();
                currentScene.setRoot(registerRoot);
            });
        } catch (Exception e) {log.error("[LoginController] Cannot load RegisterView");}


    }
    private void switchToMain(){
        //Unsubcribe khi chuyển
        ResponseDispatcher.getInstance().unsubscribe(ActionType.LOGIN, loginCallback);

        try {
            Parent mainRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.MAIN.getPath())));
            Platform.runLater(()->{
                Scene currentScene = primaryStage.getScene();
                currentScene.setRoot(mainRoot);
            });

        } catch (Exception e) {log.error("[LoginController] Cannot load Main");}


    }
}