package com.uet.BiddingApplication;

import atlantafx.base.theme.CupertinoDark;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Session.ResponseListenerThread;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.AlertUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class BiddingApplication extends Application {

    public static Stage primaryStage = null;

    @Override
    public void start(Stage stage) throws Exception {
        //Set primaryStage for easy access
        primaryStage = stage;


        // Apply CSS
        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());

        //Load FXML & create root
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPath.LOGIN.getPath()));
        Parent root = loader.load();

        // Create scene
        Scene scene = new Scene(root);

        //Add custome CSS
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app/css/brand.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app/css/main.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app/css/table.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/app/css/font.css")).toExternalForm());

        // Create stage
        Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/app/assets/logo.png")));
        stage.getIcons().add(appIcon);
        stage.setTitle("Bidding Application");
        stage.setScene(scene);

        // Set mininum dimension prevent breaking layout
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        stage.show();

        new Thread(() -> {
            ServerConnection.getInstance().connect("127.0.0.1", 8080);
        }).start();
    }

    @Override
    public void stop() throws Exception {
        // QUAN TRỌNG NHẤT: Hàm này tự động chạy khi user bấm dấu X tắt ứng dụng
        System.out.println("Đang tắt ứng dụng, dọn dẹp kết nối mạng...");

        // Cắt đứt kết nối một cách sạch sẽ, báo cho Server biết để xóa khỏi Map
        ServerConnection.getInstance().disconnect();

        // Thoát hẳn chương trình
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
