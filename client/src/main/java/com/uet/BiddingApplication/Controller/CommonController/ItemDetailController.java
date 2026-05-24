package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.Controller.SellerController.SellerItemsFormController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.SessionRegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Request.SessionTargetRequestDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.NotificationUtil;
import com.uet.BiddingApplication.Util.RegisteredSessionUtil;
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

    public void setCurrentSessionID(String sessionID){
        currentSessionID=sessionID;
        if(currentSessionID == null) log.error("[ItemDetail] ItemDetail is not contain any sessionId yet");
    }


    //--Initialize and setup--
    private final Consumer<ResponsePacket<?>> sessionDetailCallback = this::handleSessionInfoResponse;

    private final Consumer<ResponsePacket<?>> preRegisterCallback = this::handlePreRegisterResponse;
    private final Consumer<ResponsePacket<?>> delRegisterCallback = this::handleDelRegisterResponse;

    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UIUtil.roundedImageView(imgItem);
    }

    @Override
    public void onShow() {
        log.info("[ItemDetail] Đăng ký lấy thông tin chi tiết.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_SESSION_DETAIL, sessionDetailCallback);
        
        //Register/Unregister subcribe handle response
        ResponseDispatcher.getInstance().subscribe(ActionType.PRE_REGISTER_SESSION,preRegisterCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.DELETE_REGISTER_SESSION,delRegisterCallback);
        
        
        requestSessionInfo();
    }

    @Override
    public void onHide() {
        log.info("[ItemDetail] Hủy đăng ký lấy thông tin chi tiết.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_SESSION_DETAIL,sessionDetailCallback);

        //Register/Unregister unsubcribe handle response
        ResponseDispatcher.getInstance().unsubscribe(ActionType.PRE_REGISTER_SESSION,preRegisterCallback);
        ResponseDispatcher.getInstance().unsubscribe(ActionType.DELETE_REGISTER_SESSION,delRegisterCallback);
    }


    //--MAIN METHODS--

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

    //--NETWORK REQUEST--
    private void requestSessionInfo(){
        if(currentSessionID != null) {
            log.info("[ItemDetail] Đang gửi yêu cầu lấy thông tin chi tiết...");
            SessionTargetRequestDTO sessionTargetDTO = new SessionTargetRequestDTO(currentSessionID);

            RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

            request.setToken(ClientSession.getInstance().getCurrentToken());
            request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
            request.setAction(ActionType.GET_SESSION_DETAIL);
            request.setPayload(sessionTargetDTO);

            ServerConnection.getInstance().sendRequest(request);
        }
    }

    private  void requestPreRegister(String sessionId){
        log.info("[ItemDetail] Đang gửi yêu cầu đăng ký trước phiên: {}", sessionId);
        RequestPacket<SessionRegisterRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.PRE_REGISTER_SESSION);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setPayload(new SessionRegisterRequestDTO(sessionId));
        ServerConnection.getInstance().sendRequest(request);
    }

    private void requestDeleteRegister(String sessionId) {
        log.info("[ItemDetail] Đang gửi yêu cầu hủy đăng kí trc phiên: {}",sessionId);
        RequestPacket<SessionRegisterRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.DELETE_REGISTER_SESSION);
        request.setPayload(new SessionRegisterRequestDTO(sessionId));
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }

    //--HANDLE RESPONSE--
    private void handleSessionInfoResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            // Ép kiểu payload về DTO
            SessionInfoResponseDTO info = (SessionInfoResponseDTO) response.getPayload();

            //Gọi set dữ liệu lên màn hình
            setData(info);

            //Lấy giữ liệu để set up ActionButton
            handleBtnAction();
            log.info("[ItemDetail] Đã load thành công.");
        } else {
            log.error("[ItemDetail] Lỗi từ server: {}", response.getMessage());
        }
    }

    private void handlePreRegisterResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Thành công", "Đăng ký tham gia đấu giá thành công!");
            log.info("[ItemDetail] Đăng ký sản phẩm thành công.");

            //Navigate back to browse
            MainViewController.getInstance().loadView(ViewPath.BIDDER_BROWSE);

        } else {
            NotificationUtil.showError("Thất bại", response.getMessage());
            log.error("[ItemDetail] Đăng ký sản phẩm không thành công: {}", response.getMessage());
        }
    }

    private void handleDelRegisterResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Thành công", "Hủy đăng ký tham gia đấu giá thành công!");

            //Navigate back to watchlist
            MainViewController.getInstance().loadView(ViewPath.BIDDER_WATCHLIST);

        } else {
            NotificationUtil.showError("Thất bại", response.getMessage());
            log.error("[ItemDetail] Hủy đăng ký sản phẩm không thành công: {}", response.getMessage());
        }
    }

    //--BUTTON ACTION--
    private void setBtnAction(String label, String btnLabel, EventHandler<ActionEvent> actionHandler){
        Platform.runLater(()->{
            lblAction.setText(label);
            btnAction.setText(btnLabel);
            btnAction.setOnAction(actionHandler);
        });
    }

    private void handleBtnAction(){
        if(ClientSession.getInstance().getCurrentUser().getRole() == RoleType.BIDDER){
            if(RegisteredSessionUtil.getInstance().isRegistered(currentSessionID)){
                setBtnAction("Remove item from watchlist","Remove",(event)-> requestDeleteRegister(currentSessionID));
            } else{
                setBtnAction("Add item to watchlist","Add",(event)->requestPreRegister(currentSessionID));
            }
        } else if(ClientSession.getInstance().getCurrentUser().getRole() == RoleType.SELLER){
            setBtnAction("Edit this item","Edit",(event)->{
                MainViewController.getInstance().loadView(ViewPath.SELLER_ITEM_FORM,(SellerItemsFormController c) -> {
                    c.setupFormMode(currentSessionID, currentItem.getItemId());
                });
            });
        } else {log.error("[ItemDetail] RoleType is invalid.");}

    }
}
