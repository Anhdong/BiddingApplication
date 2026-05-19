package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.SessionTargetRequestDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
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
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ItemDetailController implements Initializable, ViewControllerLifecycle {
    private static final Logger log = LoggerFactory.getLogger(ItemDetailController.class);

    @FXML
    ImageView imgItem;
    @FXML Label lblName, lblStatus, lblStartDatetime, lblEndDatetime, lblStartBid, lblMinBid, lblCategory, lblAction;
    @FXML Text txtDesc;
    @FXML Button btnAction;

    private SessionInfoResponseDTO currentItem;

    //Session ID đc nhận từ bên ngoài
    String currentSessionID = null;

    public void setCurrentSessionID(String sessionID){currentSessionID=sessionID;}


    //--Initialize and setup--
    private final Consumer<ResponsePacket<?>> sessionDetailCallback = this::handleSessionInfoResponse;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(()-> UIUtil.roundedImageView(imgItem));
    }

    @Override
    public void onShow() {
        if(currentSessionID != null) {
            log.info("[ItemDetailController] Đăng ký lấy thông tin chi tiết.");
            ResponseDispatcher.getInstance().subscribe(ActionType.GET_SESSION_DETAIL, sessionDetailCallback);

            log.info("[ItemDetailController] Đang gửi yêu cầu lấy thông tin chi tiết...");
            SessionTargetRequestDTO sessionTargetDTO = new SessionTargetRequestDTO(currentSessionID);

            RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

            request.setToken(ClientSession.getInstance().getCurrentToken());
            request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
            request.setAction(ActionType.GET_SESSION_DETAIL);
            request.setPayload(sessionTargetDTO);

            ServerConnection.getInstance().sendRequest(request);
        }
    }

    @Override
    public void onHide() {
        //Hủy đăng ký
        log.info("[ItemDetailController] Hủy đăng ký lấy thông tin chi tiết.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_SESSION_DETAIL,sessionDetailCallback);
    }


    //--SET/FORMAT DATA--
    private void handleSessionInfoResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            // Ép kiểu payload về DTO
            SessionInfoResponseDTO info = (SessionInfoResponseDTO) response.getPayload();

            //Gọi set dữ liệu lên màn hình
            setData(info);
            log.info("[ItemDetailController] Đã load thành công.");
        } else {
            log.error("[ItemDetailController] Lỗi từ server: {}", response.getMessage());
        }
    }

    public void setData(SessionInfoResponseDTO sessionInfoDTO){
        Platform.runLater(()->{
            //Set dữ liệu mới
            currentItem = sessionInfoDTO;
            log.info("Item Image URL: {}", sessionInfoDTO.getImageUrl());
            if(sessionInfoDTO.getImageUrl()!= null) imgItem.setImage(new Image(sessionInfoDTO.getImageUrl()));
            lblName.setText(sessionInfoDTO.getItemName());
            lblStatus.setText(sentenceCase(sessionInfoDTO.getStatus().toString()));
            setlblDatetime(sessionInfoDTO.getStartTime(),sessionInfoDTO.getEndTime());
            lblStartBid.setText("$"+sessionInfoDTO.getStartPrice().toString());
            lblMinBid.setText("$"+sessionInfoDTO.getBidStep().toString());
            lblCategory.setText(sentenceCase(sessionInfoDTO.getCategory().name()));
            txtDesc.setText(sessionInfoDTO.getDescription());
        });
    }

    private void setlblDatetime(LocalDateTime startTime, LocalDateTime endTime){
            // Định dạng mặc định
            DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

            Platform.runLater(()->{
                lblStartDatetime.setText(startTime.format(format));
                lblEndDatetime.setText(endTime.format(format));
            });
    }

    private String sentenceCase(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    //--BUTTON ACTION--
    public void setBtnAction(String label, String btnLabel, EventHandler<ActionEvent> actionHandler){
        Platform.runLater(()->{
            lblAction.setText(label);
            btnAction.setText(btnLabel);
            btnAction.setOnAction(actionHandler);
        });
    }
}
