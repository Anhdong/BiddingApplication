package com.uet.BiddingApplication.Controller.AdminController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.RoleType;
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

public class AdminSessionsController implements Initializable, ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(AdminSessionsController.class);

    //--FXML--
    @FXML TableView<AuctionCardDTO> table;

    @FXML TableColumn<AuctionCardDTO,String> nameCol;
    @FXML TableColumn<AuctionCardDTO, SessionStatus> statusCol;
    @FXML TableColumn<AuctionCardDTO, BigDecimal> startPriceCol;
    @FXML TableColumn<AuctionCardDTO, LocalDateTime> startTimeCol;
    @FXML TableColumn<AuctionCardDTO, LocalDateTime> endTimeCol;

    // Tạo ObservableList để quản lý dữ liệu
    private ObservableList<AuctionCardDTO> dataList = FXCollections.observableArrayList();

    //--CALLBACK--
    private final Consumer<ResponsePacket<?>> AdminSessionsCallback = this::handleAdminSessionsResponse;

    //--INIT/LIFECYCLE--

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        startPriceCol.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Liên kết danh sách dữ liệu với bảng
        table.setItems(dataList);
    }

    @Override
    public void onShow() {
        log.info("[AdminSessions] Đăng ký lắng nghe danh sách phiên đấu giá.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_ALL_SESSIONS, AdminSessionsCallback);

        requestAdminSessions();
    }

    @Override
    public void onHide() {
        log.info("[AdminSessions] Hủy đăng ký lắng nghe danh sách phiên đấu giá.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_ALL_SESSIONS, AdminSessionsCallback);
    }

    //--NETWORK REQUEST--
    private void requestAdminSessions() {
        log.info("[AdminSessions] Đang gửi yêu cầu lấy danh sách phiên đấu giá...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_ALL_SESSIONS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }
    //--HANDLE RESPONSE--
    private void handleAdminSessionsResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            if(response.getPayload() == null) return;
            dataList.clear();
            dataList.addAll((List<AuctionCardDTO>) response.getPayload());

            log.info("[AdminSessions] Lấy danh sách phiên đấu giá thành công");
        } else {
            log.error("[AdminSessions] Lấy danh sách phiên đấu giá thất bại: {}", response.getMessage());
        }
    }
}