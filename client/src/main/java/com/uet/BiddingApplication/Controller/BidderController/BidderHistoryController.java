package com.uet.BiddingApplication.Controller.BidderController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class BidderHistoryController implements Initializable, ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(com.uet.BiddingApplication.Controller.BidderController.BidderHistoryController.class);

    //--FXML--
    @FXML TableView<BidderHistoryResponseDTO> table;

    @FXML TableColumn<BidderHistoryResponseDTO,String> nameCol;
    @FXML TableColumn<BidderHistoryResponseDTO, BigDecimal> myBidCol;
    @FXML TableColumn<BidderHistoryResponseDTO,BigDecimal> finalPriceCol;
    @FXML TableColumn<BidderHistoryResponseDTO,String> winnerCol;
    @FXML TableColumn<BidderHistoryResponseDTO, SessionStatus> statusCol;
    @FXML TableColumn<BidderHistoryResponseDTO, LocalDateTime> timeCol;

    // Tạo ObservableList để quản lý dữ liệu
    private ObservableList<BidderHistoryResponseDTO> dataList = FXCollections.observableArrayList();

    //--CALLBACK--
    private final Consumer<ResponsePacket<?>> BidderHistoryCallback = this::handleBidderHistoryResponse;

    //--INIT/LIFECYCLE--

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        myBidCol.setCellValueFactory(new PropertyValueFactory<>("myHighestBid"));
        finalPriceCol.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        winnerCol.setCellValueFactory(new PropertyValueFactory<>("winnerName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));

        // Liên kết danh sách dữ liệu với bảng
        table.setItems(dataList);
    }

    @Override
    public void onShow() {
        log.info("[BidderHistory] Đăng ký lắng nghe lịch sử vật phẩm.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_BIDDER_HISTORY, BidderHistoryCallback);

        requestBidderHistory();
    }

    @Override
    public void onHide() {
        log.info("[BidderHistory] Hủy đăng ký lắng nghe lịch sử.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_BIDDER_HISTORY, BidderHistoryCallback);
    }

    //--NETWORK REQUEST--
    private void requestBidderHistory() {
        log.info("[BidderHistory] Đang gửi yêu cầu lấy danh sách lịch sử vật phẩm...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_BIDDER_HISTORY);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }
    //--HANDLE RESPONSE--
    private void handleBidderHistoryResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            if(response.getPayload() == null) return;
            dataList.clear();
            dataList.addAll((List<BidderHistoryResponseDTO>) response.getPayload());

            log.info("[BidderHistory] Lấy danh sách lịch sử thành công");
        } else {
            log.error("[BidderHistory] Lấy danh sách lịch sử thất bại: {}", response.getMessage());
        }
    }
}