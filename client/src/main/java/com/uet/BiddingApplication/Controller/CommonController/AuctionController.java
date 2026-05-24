package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.SessionTargetRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionRoomSyncDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.UIUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AuctionController implements Initializable, ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    //--FXML--
    @FXML ImageView imgItem;
    @FXML Label lblName, lblTimer, lblCurrentBid, lblBidder;
    @FXML Text txtDesc;
    @FXML LineChart lcHistory;
    @FXML TextField txtMinBid, txtMaxBid, txtBidAmount;
    @FXML ToggleButton btnAutoBid;
    @FXML Button btnManualBid;
    @FXML VBox vbxAutoBid, vbxManualBid;

    //--FIELDS--
    private long remainingTime;
    private LocalDateTime endTime;
    private BigDecimal minBid = null;
    private List<BidHistoryDTO> history;

    //--STATE--
    private String currentSessionId = null;

    public void setCurrentSessionId(String sessionId){this.currentSessionId = sessionId;}

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> joinSessionCallback = this::handleJoinSessionResponse;

    //--INIT--
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if(ClientSession.getInstance().getCurrentUser().getRole() != RoleType.BIDDER){
            log.info("[Auction] Prepare auction room for seller");
            vbxAutoBid.setManaged(false);
            vbxAutoBid.setVisible(false);

            vbxManualBid.setManaged(false);
            vbxManualBid.setVisible(false);
        } else log.info("[Auction] Prepare auction room for bidder");

        UIUtil.roundedImageView(imgItem);
    }

    @Override
    public void onShow() {
        ResponseDispatcher.getInstance().subscribe(ActionType.JOIN_SESSION, joinSessionCallback);

        //Request join room
        requestJoinSession();
        requestSubscribeRealtime();
    }

    @Override
    public void onHide() {
        ResponseDispatcher.getInstance().unsubscribe(ActionType.JOIN_SESSION, joinSessionCallback);

        //Request leave room
        requestLeaveSession();
        requestUnsubscribeRealtime();
    }

    //--MAIN METHODS--
    private void setData(AuctionRoomSyncDTO dto){
        remainingTime = dto.getRemainingMillis();

        minBid = dto.getBidStep();

        lblCurrentBid.setText(dto.getCurrentPrice().toString());
        lblBidder.setText(dto.getHighestBidderName());

        history = dto.getHistory();

        //set lbl name and desc;
        if(dto.getImageURL()!= null) imgItem.setImage(new Image(dto.getImageURL()));
    }

    //--NETWORK REQUEST--
    //ROOM ACTION
    private void requestJoinSession(){
        log.info("[Auction] Gửi yêu cầu vào phòng");

        RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.JOIN_SESSION);
        request.setPayload(new SessionTargetRequestDTO(currentSessionId));

        ServerConnection.getInstance().sendRequest(request);
    }
    private void requestLeaveSession(){
        log.info("[Auction] Gửi yêu cầu rời phòng");

        RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.LEAVE_SESSION);
        request.setPayload(new SessionTargetRequestDTO(currentSessionId));

        ServerConnection.getInstance().sendRequest(request);
    }
    private void requestSubscribeRealtime(){
        log.info("[Auction] Gửi yêu cầu đăng ký nhận thông báo");

        RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.SUBSCRIBE_REALTIME);
        request.setPayload(new SessionTargetRequestDTO(currentSessionId));

        ServerConnection.getInstance().sendRequest(request);
    }
    private void requestUnsubscribeRealtime(){
        log.info("[Auction] Gửi yêu cầu hủy đăng ký nhận thông báo");

        RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.UNSUBSCRIBE_REALTIME);
        request.setPayload(new SessionTargetRequestDTO(currentSessionId));

        ServerConnection.getInstance().sendRequest(request);
    }


    //--HANDLE RESPONSE--
    private void handleJoinSessionResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            // Ép kiểu payload về DTO
            AuctionRoomSyncDTO info = (AuctionRoomSyncDTO) response.getPayload();

            //Gọi set dữ liệu lên màn hình
            setData(info);

            log.info("[Auction] Đã load thành công.");
        } else {
            log.error("[Aution Lỗi từ server: {}", response.getMessage());
        }
    }
}
