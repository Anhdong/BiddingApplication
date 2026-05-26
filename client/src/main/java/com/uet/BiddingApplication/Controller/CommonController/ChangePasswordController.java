package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.PasswordChangeRequestDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.NotificationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;

import java.util.Objects;
import java.util.function.Consumer;

public class ChangePasswordController implements ViewControllerLifecycle {
    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChangePasswordController.class);
    //--FXML--
    @FXML private PasswordField txtOldPassword, txtNewPassword;

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> changePasswordCallback = this::handleChangePasswordResponse;


    //--INIT/LIFECYCLE--
    @Override
    public void onShow(){
        ResponseDispatcher.getInstance().subscribe(ActionType.CHANGE_PASSWORD, changePasswordCallback);
    }

    @Override
    public void onHide() {
        ResponseDispatcher.getInstance().unsubscribe(ActionType.CHANGE_PASSWORD, changePasswordCallback);
    }

    //--MAIN METHODS--
    @FXML
    private void handleChangePassword(){
        //Check validate
        if(!validateInput()) return;

        RequestPacket<PasswordChangeRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.CHANGE_PASSWORD);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setPayload(new PasswordChangeRequestDTO(txtOldPassword.getText(),txtNewPassword.getText()));

        ServerConnection.getInstance().sendRequest(request);
        log.info("[ChangePassword] Send request change password");

    }


    private boolean validateInput(){
        if(txtNewPassword.getText().isEmpty() || txtOldPassword.getText().isEmpty()){
            NotificationUtil.showError("Password cannot be empty.");
            return false;}
        if(Objects.equals(txtNewPassword.getText(), txtOldPassword.getText())){
            NotificationUtil.showError("Old and New Password cannot be the same.");
            return false;}
        return true;
    }

    private void handleChangePasswordResponse(ResponsePacket<?> response){
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Success","Change password successfully!");
            log.info("[ChangePassword] Password change successfully");
        } else {
            NotificationUtil.showError(response.getMessage());
            log.error("[ChangePassword] {}",response.getMessage());
        }
    }
}

