package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.Controller.BidderController.BidderBrowseController;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class ItemCardController implements Initializable {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(ItemCardController.class);

    //--FXML--
    @FXML private VBox vbxCard;
    @FXML private ImageView imgItem;
    @FXML private Label lblName, lblDatetime, lblPrice;
    @FXML private Button btnAction;

    private AuctionCardDTO currentItem;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(()->UIUtil.roundedImageView(imgItem));
    }

    //--SET/FORMAT DATA
    public void setData(AuctionCardDTO cardDto){
        Platform.runLater(()->{
            currentItem = cardDto;
            if(cardDto.getImageURL()!= null) imgItem.setImage(new Image(cardDto.getImageURL()));
            lblName.setText(cardDto.getItemName());
            setlblDatetime(cardDto.getStatus(),cardDto.getStartTime(),cardDto.getEndTime());
            lblPrice.setText("$"+cardDto.getStartPrice().toString());
        });
    }

    private void setlblDatetime(SessionStatus status,LocalDateTime startTime, LocalDateTime endTime){
        if (Objects.requireNonNull(status) == SessionStatus.OPEN) {
            // Định dạng giờ:phút
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            // Định dạng ngày:tháng/năm
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            LocalDateTime now = LocalDateTime.now();
            //Xu ly format neu cung ngay hoac khac ngay
            Platform.runLater(()->{
                if (now.toLocalDate().equals(startTime.toLocalDate())) {
                    lblDatetime.setText(startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter));
                } else {
                    lblDatetime.setText(startTime.format(timeFormatter) + " - " + endTime.format(dateFormatter));
                }
            });
        } else {
            Platform.runLater(()->{lblDatetime.setText(sentenceCase((status.toString())));});
        }
    }

    private String sentenceCase(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    //--CARD ACTION--
    public void setCardAction(EventHandler<MouseEvent> actionHandler){
        vbxCard.setOnMouseClicked(actionHandler);
    }

    //--BUTTON ACTION--
    public void setBtnAction(String label, EventHandler<ActionEvent> actionHandler){
        Platform.runLater(()->{
            btnAction.setText(label);
            btnAction.setOnAction(actionHandler);
        });
    }
    public void setButtonVisible(boolean visible) {
        btnAction.setVisible(visible);
        btnAction.setManaged(visible);
    }

}
