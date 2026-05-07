package com.uet.BiddingApplication.Controller.AuthController;

import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class RegisterController implements Initializable {

    @FXML private TextField txtUsername, txtEmail, txtPhoneNumber;
    @FXML private PasswordField txtPassword;
    @FXML private ToggleGroup roleGroup;
    @FXML private RadioButton rbBidder, rbSeller;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Gán "giá trị thực" cho mỗi nút để dễ lấy sau này
        rbBidder.setUserData(RoleType.BIDDER);
        rbSeller.setUserData(RoleType.SELLER);
    }

    public void switchToLogin() {
    Parent loginRoot = null;
    try {
        loginRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.LOGIN.getPath())));
    } catch (Exception e) {System.out.println("[RegisterController] Cannot load LoginView");}

    Scene currentScene = primaryStage.getScene();
    currentScene.setRoot(loginRoot);
}
    //Main func
    @FXML
    private void handleRegister(){
        if(validateInput()){
            switchToLogin();
        }
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

    private void sendRegisterRequest(){}

    private void handleRegisterResponse(){}

}
