package com.uet.BiddingApplication.Controller.AuthController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;

import java.util.Objects;

public class RegisterController {

    public void SwitchToLogin(MouseEvent event) {
    Parent loginRoot = null;
    try {
        loginRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/app/fxml/AuthView/LoginView.fxml")));
    } catch (Exception e) {e.printStackTrace();}

    Scene currentScene = ((Node) event.getSource()).getScene();
    currentScene.setRoot(loginRoot);

}


}
