package com.uet.BiddingApplication.controller.AuthController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;;

public class LoginController {

    public void SwitchToRegister(MouseEvent event) {
        Parent registerRoot = null;
        try {
            registerRoot = FXMLLoader.load(getClass().getResource("/app/fxml/AuthView/RegisterView.fxml"));
        } catch (Exception e) {e.printStackTrace();}

        Scene currentScene = ((Node) event.getSource()).getScene();
        currentScene.setRoot(registerRoot);

    }
}