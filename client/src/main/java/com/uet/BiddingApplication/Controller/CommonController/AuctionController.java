package com.uet.BiddingApplication.Controller.CommonController;

import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.AutoBidRegisterDTO;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;
import com.uet.BiddingApplication.DTO.Request.SessionTargetRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionRoomSyncDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.RealtimeUpdateDTO;
import com.uet.BiddingApplication.DTO.Response.SessionResultDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.BidType;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Model.AutoBidSetting;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.NotificationUtil;
import com.uet.BiddingApplication.Util.UIUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AuctionController implements Initializable, ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    //--FXML--
    @FXML ImageView imgItem;
    @FXML Label lblName, lblTimer, lblCurrentBid, lblBidder;
    @FXML Text txtDesc;
    @FXML LineChart<String,Number> lcHistory;
    @FXML TextField txtBidStep, txtMaxBid, txtBidAmount;
    @FXML ToggleButton btnAutoBid;
    @FXML Button btnManualBid;
    @FXML VBox vbxAutoBid, vbxManualBid;

    //--FIELDS--
    private long endTimeMillis;
    private Timeline countdownTimeline;

    private XYChart.Series<String, Number> bidHistorySeries;
    private BigDecimal currentBid = new BigDecimal(0);
    private BigDecimal minBid;


    //--STATE--
    private String currentSessionId = null;

    public void setCurrentSessionId(String sessionId){this.currentSessionId = sessionId;}

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> joinSessionCallback = this::handleJoinSessionResponse;
    private final Consumer<ResponsePacket<?>> priceUpdateCallback = this::handlePriceUpdateResponse;
    private final Consumer<ResponsePacket<?>> sessionEndCallback = this::handleSessionEndResponse;
    private final Consumer<ResponsePacket<?>> autoBidCancelCallback = this::handleAutoBidCancelResponse;

    private final Consumer<ResponsePacket<?>> placeManualBidCallback = this::handlePlaceManualBidResponse;
    //--INIT--
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Check RoleType to hide bid elements
        if(ClientSession.getInstance().getCurrentUser().getRole() != RoleType.BIDDER){
            log.info("[Auction] Prepare auction room for seller");
            vbxAutoBid.setManaged(false);
            vbxAutoBid.setVisible(false);

            vbxManualBid.setManaged(false);
            vbxManualBid.setVisible(false);
        } else log.info("[Auction] Prepare auction room for bidder");

        btnAutoBid.textProperty().bind(
                javafx.beans.binding.Bindings.when(btnAutoBid.selectedProperty())
                        .then("Enable")
                        .otherwise("Disable")
        );

        UIUtil.roundedImageView(imgItem);
    }

    @Override
    public void onShow() {
        ResponseDispatcher.getInstance().subscribe(ActionType.JOIN_SESSION, joinSessionCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.REALTIME_PRICE_UPDATE, priceUpdateCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.REALTIME_SESSION_END, sessionEndCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.AUTO_BID_CANCEL, autoBidCancelCallback);

        ResponseDispatcher.getInstance().subscribe(ActionType.PLACE_MANUAL_BID, placeManualBidCallback);

        //Request join room
        requestJoinSession();
        requestSubscribeRealtime();
    }

    @Override
    public void onHide() {
        //Turn of TimeLine CountDown
        if (countdownTimeline != null) countdownTimeline.stop();

        ResponseDispatcher.getInstance().unsubscribe(ActionType.JOIN_SESSION, joinSessionCallback);
        ResponseDispatcher.getInstance().unsubscribe(ActionType.REALTIME_PRICE_UPDATE, priceUpdateCallback);
        ResponseDispatcher.getInstance().unsubscribe(ActionType.REALTIME_SESSION_END, sessionEndCallback);
        ResponseDispatcher.getInstance().unsubscribe(ActionType.AUTO_BID_CANCEL, autoBidCancelCallback);

        ResponseDispatcher.getInstance().unsubscribe(ActionType.PLACE_MANUAL_BID, placeManualBidCallback);

        //Request leave room
        requestLeaveSession();
        requestUnsubscribeRealtime();
    }

    //--MAIN METHODS--
    private void setData(AuctionRoomSyncDTO dto){
        log.info("[Auction] Thiết lập giao diện phòng đấu giá");

        //Coundown Timer
        if (dto.getRemainingMillis() > 0) {
            startCountdownTimer(dto.getRemainingMillis());
        } else {
            lblTimer.setText("Auction End!");
        }

        minBid = dto.getBidStep();
        if (dto.getCurrentPrice() != null) currentBid = dto.getCurrentPrice();

        Platform.runLater(()->{
            if(dto.getCurrentPrice() != null) lblCurrentBid.setText("$"+dto.getCurrentPrice().toString());
            if(dto.getHighestBidderName() != null) lblBidder.setText(dto.getHighestBidderName());
            lblName.setText(dto.getItemName());
            txtDesc.setText(dto.getDescription());

            //Khoi phuc set up nut auto Bid
            AutoBidSetting autoBidSetting = dto.getAutoBidSetting();
            if(autoBidSetting != null) {
                txtBidStep.setText(autoBidSetting.getIncrement().toString());
                txtMaxBid.setText(autoBidSetting.getMaxBid().toString());
                btnAutoBid.setSelected(false);
            } else btnAutoBid.setSelected(true);

            // Khởi tạo series
            bidHistorySeries = new XYChart.Series<>();
            lcHistory.getData().clear();
            lcHistory.getData().add(bidHistorySeries);

            if (dto.getImageURL() != null) imgItem.setImage(new Image(dto.getImageURL()));
        });

        for (BidHistoryDTO newBid : dto.getHistory().reversed()) handleNewBidHistory(newBid);
    }

    private void startCountdownTimer(long remainingMillis) {
        // 1. Dừng timer cũ nếu có (phòng trường hợp bấm linh tinh)
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        // 2. Tính ra chính xác thời điểm kết thúc (tuyệt đối)
        endTimeMillis = System.currentTimeMillis() + remainingMillis;

        // 3. Tạo một Timeline chạy lặp lại mỗi 1 giây (1000 ms)
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Lấy thời gian hiện tại
            long currentTime = System.currentTimeMillis();
            long timeLeft = endTimeMillis - currentTime;

            // Nếu hết giờ
            if (timeLeft <= 0) {
                lblTimer.setText("00:00:00");
                // Dừng đồng hồ
                countdownTimeline.stop();
                // Có thể xử lý thêm: Khóa nút Đặt giá, hiển thị chữ "Đã kết thúc" v.v.
                return;
            }

            long seconds = timeLeft / 1000;

            // Công thức tính Ngày, Giờ, Phút, Giây
            long d = seconds / 86400;
            long h = (seconds % 86400) / 3600;
            long m = (seconds % 3600) / 60;
            long s = seconds % 60;
            // Cập nhật giao diện
            if (d > 0) {
                lblTimer.setText(String.format("%d days - %02d:%02d:%02d", d, h, m, s));
            } else {
                lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }));

        // Cho phép Timeline chạy lặp vô hạn (cho đến khi ta gọi .stop() ở trên)
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);

        // Bắt đầu chạy
        countdownTimeline.play();
    }

    private void handleNewBidHistory(BidHistoryDTO newBid) {
        Platform.runLater(() -> {

            currentBid = newBid.getBidAmount();
            lblCurrentBid.setText("$"+currentBid.toString());
            lblBidder.setText(newBid.getBidderName());

            // 1. Format thời gian thành chuỗi (ví dụ: "14:30:15") làm nhãn trục X
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String timeStr = newBid.getTime().format(formatter);
            Number price = currentBid;

            // Create new data point
            XYChart.Data<String, Number> newDataPoint = new XYChart.Data<>(timeStr, price);

            if (bidHistorySeries != null) {
                bidHistorySeries.getData().add(newDataPoint);

                if (bidHistorySeries.getData().size() > 10) {
                    bidHistorySeries.getData().removeFirst();
                }
            }
        });
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

    //Bid Action
    private void requestPlaceManualBid(){
        log.info("[Auction] Gửi yêu cầu đấu giá với mức giá: {}",txtBidAmount.getText());
        RequestPacket<BidRequestDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.PLACE_MANUAL_BID);
        request.setPayload(new BidRequestDTO(currentSessionId,new BigDecimal(txtBidAmount.getText()), BidType.MANUAL));

        ServerConnection.getInstance().sendRequest(request);
    }
    private void requestRegisterAutoBid(){
        log.info("[Auction] Gửi yêu cầu đăng ký Auto Bid");
        RequestPacket<AutoBidRegisterDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.REGISTER_AUTO_BID);
        request.setPayload(new AutoBidRegisterDTO(
                currentSessionId, new BigDecimal(txtMaxBid.getText()), new BigDecimal(txtBidStep.getText())));

        ServerConnection.getInstance().sendRequest(request);
    }
    private void requestCancelAutoBid(){
        log.info("[Auction] Gửi yêu cầu hủy đăng ký Auto Bid");
        RequestPacket<SessionTargetRequestDTO> request = new RequestPacket<>();

        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setAction(ActionType.CANCEL_AUTO_BID);
        request.setPayload(new SessionTargetRequestDTO(currentSessionId));

        ServerConnection.getInstance().sendRequest(request);
    }


    //--HANDLE RESPONSE--
    private void handleJoinSessionResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            AuctionRoomSyncDTO info = (AuctionRoomSyncDTO) response.getPayload();
            setData(info);
            log.info("[Auction] Đã load thành công.");
        } else {
            log.error("[Aution] Lỗi từ server: {}", response.getMessage());
        }
    }

    private void handlePriceUpdateResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            RealtimeUpdateDTO dto = (RealtimeUpdateDTO) response.getPayload();

            handleNewBidHistory(dto.getLastBid());
            startCountdownTimer(dto.getRemainingMillis());

            log.info("[Auction] Cập nhật giá thành công.");
        } else {
            log.error("[Aution] Cập nhật giá thất bại: {}", response.getMessage());
        }
    }

    private void handleSessionEndResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            SessionResultDTO info = (SessionResultDTO) response.getPayload();
            NotificationUtil.showInfo("The auction has ended!");
            RoleType role=  ClientSession.getInstance().getCurrentUser().getRole();

            //Navigate user back to page
            if(role == RoleType.BIDDER) MainViewController.getInstance().loadView(ViewPath.BIDDER_WATCHLIST);
            else if (role == RoleType.SELLER) MainViewController.getInstance().loadView(ViewPath.SELLER_ITEMS);

            log.info("[Auction] Phiên đấu giá kết thúc thành công");
        } else {
            log.error("[Aution] Phiên đấu giá kết thúc không thành công: {}", response.getMessage());
        }
    }

    private void handleAutoBidCancelResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            Platform.runLater(() -> btnAutoBid.setSelected(true));
            log.info("[Auction] Hủy đăng kí Auto Bid do vượt quá Max Bid");
        } else {
            log.error("[Aution] Không thể hủy đăng kí Auto Bid {}", response.getMessage());
        }
    }

    //Place Manual Bid Reponse
    private void handlePlaceManualBidResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            log.info("[Auction] Đặt giá thành công!");
        } else {
            NotificationUtil.showError(response.getMessage());
            log.error("[Aution] Không thể đặt giá{}", response.getMessage());
        }
    }

    //--BUTTON ACTION--
    @FXML
    public void handleManualBid(){
        if(txtBidAmount.getText().isEmpty()) {
            NotificationUtil.showError("Bid amount cannot be empty.");
            return;
        }
        try {
            BigDecimal manualBid = new BigDecimal(txtBidAmount.getText());
            if(manualBid.compareTo(currentBid) <= 0) {
                NotificationUtil.showError("Bid amount cannot be smaller or equal than current bid");
                return;
            }

            requestPlaceManualBid();
        } catch (NumberFormatException e) {
            NotificationUtil.showError("Bid amount is invalid!");
        }

    }

    @FXML
    public void handleAutoBid(){
        // Selected == orange == enable == isDisable
        if (btnAutoBid.isSelected()) {
            requestCancelAutoBid();
            return;
        }

        if(txtMaxBid.getText().isEmpty() || txtBidStep.getText().isEmpty()) {
            NotificationUtil.showError("Bid increment or Max Bid cannot be empty");
            btnAutoBid.setSelected(true);
            return;
        }

        try {
            BigDecimal step = new BigDecimal(txtBidStep.getText());
            if(step.compareTo(minBid) < 0) {
                NotificationUtil.showError("Bid step amount cannot be smaller than session minimum bid");
                btnAutoBid.setSelected(true);
                return;
            }
            requestRegisterAutoBid();

        } catch (NumberFormatException e) {
            NotificationUtil.showError("Number only");
            btnAutoBid.setSelected(true);
        }
    }
}
