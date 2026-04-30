package com.uet.BiddingApplication.Controller.AuthController;

import com.uet.BiddingApplication.Controller.Enum.ViewPath;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;

import javax.swing.text.View;
import java.util.Objects;

public class LoginController {

    public void SwitchToRegister(MouseEvent event) {
        Parent registerRoot = null;
        try {
            registerRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(ViewPath.REGISTER.getPath())));
        } catch (Exception e) {e.printStackTrace();}

        Scene currentScene = ((Node) event.getSource()).getScene();
        currentScene.setRoot(registerRoot);

    }
}