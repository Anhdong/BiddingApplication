package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Util.UIUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class ItemCardController implements Initializable {

    @FXML private VBox vbxCard;
    @FXML private ImageView imgItem;
    @FXML private Label lblName, lblDate, lblPrice;
    @FXML private Button btnAction;

    private AuctionCardDTO currentItem;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(()->{UIUtil.roundedImageView(imgItem);});
    }

    public void setData(AuctionCardDTO cardDto){
        Platform.runLater(()->{
            currentItem = cardDto;
            if(cardDto.getImageURL()!= null) imgItem.setImage(new Image(cardDto.getImageURL()));
            lblName.setText(cardDto.getItemName());
            setLblDate(cardDto.getStatus(),cardDto.getStartTime(),cardDto.getEndTime());
            lblPrice.setText("$"+cardDto.getStartPrice().toString());
        });
    }

    private void setLblDate(SessionStatus status,LocalDateTime startTime, LocalDateTime endTime){
        if (Objects.requireNonNull(status) == SessionStatus.OPEN) {
            // Định dạng giờ:phút
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            // Định dạng ngày:tháng/năm
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            LocalDateTime now = LocalDateTime.now();
            //Xu ly format neu cung ngay hoac khac ngay
            Platform.runLater(()->{
                if (now.toLocalDate().equals(startTime.toLocalDate())) {
                    lblDate.setText(startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter));
                } else {
                    lblDate.setText(startTime.format(timeFormatter) + " - " + endTime.format(dateFormatter));
                }
            });
        } else {
            Platform.runLater(()->{lblDate.setText(sentenceCase((status.toString())));});
        }
    }

    private String sentenceCase(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    //Nhan logic tu browse
    public void setBtnAction(String label, EventHandler<ActionEvent> actionHandler){
        Platform.runLater(()->{
            btnAction.setText(label);
            btnAction.setOnAction(actionHandler);
        });
    }

}
