package com.uet.BiddingApplication.controller.AuthController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;

public class RegisterController {

    public void SwitchToLogin(MouseEvent event) {
    Parent loginRoot = null;
    try {
        loginRoot = FXMLLoader.load(getClass().getResource("/app/fxml/AuthView/LoginView.fxml"));
    } catch (Exception e) {e.printStackTrace();}

    Scene currentScene = ((Node) event.getSource()).getScene();
    currentScene.setRoot(loginRoot);

}


}
