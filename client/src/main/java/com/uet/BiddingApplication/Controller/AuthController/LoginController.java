package com.uet.BiddingApplication.Controller.AuthController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;

import java.util.Objects;

public class LoginController {

    public void SwitchToRegister(MouseEvent event) {
        Parent registerRoot = null;
        try {
            registerRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/app/fxml/AuthView/RegisterView.fxml")));
        } catch (Exception e) {e.printStackTrace();}

        Scene currentScene = ((Node) event.getSource()).getScene();
        currentScene.setRoot(registerRoot);

    }
}