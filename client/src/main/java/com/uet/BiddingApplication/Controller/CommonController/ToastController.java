package com.uet.BiddingApplication.Controller.CommonController;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class ToastController {
    @FXML private HBox rootBox;
    @FXML private FontIcon icon;
    @FXML private Label lblTitle;
    @FXML private Label lblContent;

    public void setNotification(String title, String content, String iconLiteral, String iconColorClass) {
        if(title != null) lblTitle.setText(title);
        if(content != null) lblContent.setText(content);
        if(iconLiteral != null) icon.setIconLiteral(iconLiteral);
        if(iconColorClass != null) {
            icon.getStyleClass().add(iconColorClass);
        }
    }
}
