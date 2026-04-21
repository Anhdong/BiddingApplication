package com.uet.BiddingApplication;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class BiddingApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Apply CSS
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());

        //Load FXML & create root
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/fxml/Auth/RegisterView.fxml"));
        Parent root = loader.load();

        // Create scene
        Scene scene = new Scene(root);

        //Add custome CSs
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app/css/main.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app/css/font.css")).toExternalForm());

        // Create stage
        stage.setTitle("Bidding Application");
        stage.setScene(scene);

        // Set mininum dimension prevent breaking layout
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
