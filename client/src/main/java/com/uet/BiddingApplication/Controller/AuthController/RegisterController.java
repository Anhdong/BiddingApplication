package com.uet.BiddingApplication.Controller.AuthController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.AlertUtil;
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

public class RegisterController implements Initializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField txtUsername, txtEmail, txtPhoneNumber;
    @FXML private PasswordField txtPassword;
    @FXML private ToggleGroup roleGroup;
    @FXML private RadioButton rbBidder, rbSeller;

    private final Consumer<ResponsePacket<?>> registerCallback = this::handleRegisterResponse;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Gán "giá trị thực" cho mỗi nút để dễ lấy sau này
        rbBidder.setUserData(RoleType.BIDDER);
        rbSeller.setUserData(RoleType.SELLER);

        //Subscribe to listener
        ResponseDispatcher.getInstance().subscribe(ActionType.REGISTER, registerCallback);
    }


    //Main func
    @FXML
    private void handleRegister(){
        //Check validate
        if(!validateInput()) return;

        //Get info
        RegisterRequestDTO registerDTO = getRegisterRequestDTO();

        RequestPacket<RegisterRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.REGISTER);
        request.setPayload(registerDTO);

        //Send Request
        ServerConnection.getInstance().sendRequest(request);

    }

    private RegisterRequestDTO getRegisterRequestDTO() {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String phone = txtPhoneNumber.getText();
        String password = txtPassword.getText();
        RoleType role = (RoleType) roleGroup.getSelectedToggle().getUserData();

        //Wrap in DTO
        RegisterRequestDTO registerDTO = new RegisterRequestDTO();
        registerDTO.setUsername(username);
        registerDTO.setEmail(email);
        registerDTO.setPhone(phone);
        registerDTO.setPassword(password);
        registerDTO.setRole(role);
        return registerDTO;
    }

    private boolean validateInput(){
        if(txtUsername.getText().isEmpty()){
            AlertUtil.showAlert("Username cannot be empty.");
            return false;
        }
        if(txtEmail.getText().isEmpty() || !txtEmail.getText().matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")){
            AlertUtil.showAlert("Email cannot be empty or is invalid.");
            return false;
        }
        if(txtPhoneNumber.getText().isEmpty() || !txtPhoneNumber.getText().matches("\\d+")){
            AlertUtil.showAlert("Phone number cannot be empty or can only contain numbers.");
            return false;
        }
        if(txtPassword.getText().isEmpty()){
            AlertUtil.showAlert("Password cannot be empty.");
            return false;
        }
        if(roleGroup.getSelectedToggle() == null){
            AlertUtil.showAlert("Role cannot be empty.");
            return false;
        }

        return true;}

    private void handleRegisterResponse(ResponsePacket<?> response){
        if (response.getStatusCode() == 200) {
            AlertUtil.showAlert("Success","Account created successfully!");
            switchToLogin();
        } else {
            AlertUtil.showAlert(response.getMessage());
        }
    }

    public void switchToLogin() {
        //Unsubcribe khi chuyen di
        ResponseDispatcher.getInstance().unsubscribe(ActionType.REGISTER, registerCallback);

        Parent loginRoot = null;
        try {
            loginRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.LOGIN.getPath())));
        } catch (Exception e) {log.info("[RegisterController] Cannot load LoginView");}

        Scene currentScene = primaryStage.getScene();
        currentScene.setRoot(loginRoot);
    }
}