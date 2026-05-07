package com.uet.BiddingApplication.Controller.AuthController;

import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Objects;

import static com.uet.BiddingApplication.BiddingApplication.primaryStage;

public class LoginController {


    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    public void switchToRegister() {
        Parent registerRoot = null;
        try {
            registerRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.REGISTER.getPath())));
        } catch (Exception e) {System.out.println("[LoginController] Cannot load RegisterView");}

        Scene currentScene = primaryStage.getScene();
        currentScene.setRoot(registerRoot);
    }

    //Main func
    @FXML
    private void handleLogin(){
        if(validateInput()){

        }
    }

    private boolean validateInput(){
        if(txtUsername.getText().isEmpty()){
            AlertUtil.showAlert("Username cannot be empty.");
            return false;
        }
        if(txtPassword.getText().isEmpty()){
            AlertUtil.showAlert("Password cannot be empty.");
            return false;
        }

        return true;}

    private void sendLoginRequest(){}

    private void handleLoginResponse(){}

    private void switchToMain(){}
}