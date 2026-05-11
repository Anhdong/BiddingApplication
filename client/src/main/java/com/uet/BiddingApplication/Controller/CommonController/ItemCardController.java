package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Util.UIUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ItemCardController implements Initializable {

    @FXML private VBox vbxCard;
    @FXML private ImageView imgItem;
    @FXML private Label lblName, lblDate, lblPrice;
    @FXML private Button btnAction;

    private AuctionCardDTO currentItem;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UIUtil.roundedImageView(imgItem);
    }

    public void setData(AuctionCardDTO cardDto){
        currentItem = cardDto;
        lblName.setText(cardDto.getItemName());
        //TODO: apply time format rely on status+ convert IMGURL to image
        //lblDate.setText("hh:mm");
        lblPrice.setText("$"+cardDto.getStartPrice().toString());
    }

    //Nhan logic tu browse
    public void setBtnAction(String label, EventHandler<ActionEvent> actionHandler){
        btnAction.setText(label);
        btnAction.setOnAction(actionHandler);
    }

}
