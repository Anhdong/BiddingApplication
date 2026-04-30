package com.uet.BiddingApplication.Controller.AuthController;

import com.uet.BiddingApplication.Controller.Enum.ViewPath;
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
        loginRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.LOGIN.getPath())));
    } catch (Exception e) {e.printStackTrace();}

    Scene currentScene = ((Node) event.getSource()).getScene();
    currentScene.setRoot(loginRoot);

}


}
