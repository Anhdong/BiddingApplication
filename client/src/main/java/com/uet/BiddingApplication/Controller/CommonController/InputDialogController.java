package com.uet.BiddingApplication.Controller.CommonController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class InputDialogController implements Initializable {

    @FXML DialogPane dialogPane;
    @FXML Label label;
    @FXML FontIcon icon;
    @FXML PasswordField txtInput;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dialogPane.setExpandableContent(null);
    }

    public void setupInterface(String title, String iconLiteral){
        if(label != null) label.setText(title);
        if(iconLiteral != null) icon.setIconLiteral(iconLiteral);
    }

    public String getInputValue() {
        if (txtInput != null) {
            return txtInput.getText();
        }
        return "";
    }
}
