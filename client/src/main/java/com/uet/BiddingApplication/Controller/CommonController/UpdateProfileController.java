package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.PasswordChangeRequestDTO;
import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class UpdateProfileController implements Initializable ,ViewControllerLifecycle {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UpdateProfileController.class);
    //--FXML--
    @FXML private TextField txtUsername, txtPhoneNumber;

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> updateProfileCallback = this::handleUpdateProfileResponse;


    //--INIT/LIFECYCLE--


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        txtUsername.setText(ClientSession.getInstance().getCurrentUser().getUsername());
        txtPhoneNumber.setText(ClientSession.getInstance().getCurrentUser().getPhone());
    }

    @Override
    public void onShow(){
        ResponseDispatcher.getInstance().subscribe(ActionType.UPDATE_PROFILE, updateProfileCallback);
    }

    @Override
    public void onHide() {
        ResponseDispatcher.getInstance().unsubscribe(ActionType.UPDATE_PROFILE, updateProfileCallback);
    }

    //--MAIN METHODS--
    @FXML
    private void handleUpdateProfile(){
        //Check validate
        if(!validateInput()) return;

        RequestPacket<ProfileUpdateRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.UPDATE_PROFILE);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setPayload(new ProfileUpdateRequestDTO(txtUsername.getText(),txtPhoneNumber.getText(),null));

        ServerConnection.getInstance().sendRequest(request);
        log.info("[ChangePassword] Send request update profile");

    }

    private boolean validateInput(){
        if(txtUsername.getText().isEmpty()){
            NotificationUtil.showError("Username cannot be empty.");
            return false;
        }
        if(txtPhoneNumber.getText().isEmpty() || !txtPhoneNumber.getText().matches("\\d+")){
            NotificationUtil.showError("Phone number cannot be empty or can only contain numbers.");
            return false;
        }

        return true;
    }

    private void handleUpdateProfileResponse(ResponsePacket<?> response){
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Success","Update profile successfully!");
            log.info("[UpdateProfile] Update profile successfully");
        } else {
            NotificationUtil.showError(response.getMessage());
            log.error("[UpdateProfile] {}",response.getMessage());
        }
    }
}