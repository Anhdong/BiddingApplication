package com.uet.BiddingApplication.Controller.AdminController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
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

public class AdminUsersController implements Initializable, ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(AdminUsersController.class);

    //--FXML--
    @FXML TableView<UserProfileDTO> table;

    @FXML TableColumn<UserProfileDTO,String> idCol;
    @FXML TableColumn<UserProfileDTO, String> usernameCol;
    @FXML TableColumn<UserProfileDTO, RoleType> roleCol;
    @FXML TableColumn<UserProfileDTO,String> emailCol;
    @FXML TableColumn<UserProfileDTO, String> phoneCol;

    // Tạo ObservableList để quản lý dữ liệu
    private ObservableList<UserProfileDTO> dataList = FXCollections.observableArrayList();

    //--CALLBACK--
    private final Consumer<ResponsePacket<?>> AdminUsersCallback = this::handleAdminUsersResponse;

    //--INIT/LIFECYCLE--

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        // Liên kết danh sách dữ liệu với bảng
        table.setItems(dataList);
    }

    @Override
    public void onShow() {
        log.info("[AdminUsers] Đăng ký lắng nghe danh sách người dùng.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_ALL_USERS, AdminUsersCallback);

        requestAdminUsers();
    }

    @Override
    public void onHide() {
        log.info("[AdminUsers] Hủy đăng ký lắng nghe danh sách người dùng.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_ALL_USERS, AdminUsersCallback);
    }

    //--NETWORK REQUEST--
    private void requestAdminUsers() {
        log.info("[AdminUsers] Đang gửi yêu cầu lấy danh sách người dùng...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_ALL_USERS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }
    //--HANDLE RESPONSE--
    private void handleAdminUsersResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            if(response.getPayload() == null) return;
            dataList.clear();
            dataList.addAll((List<UserProfileDTO>) response.getPayload());

            log.info("[AdminUsers] Lấy danh sách người dùng thành công");
        } else {
            log.error("[AdminUsers] Lấy danh sách người dùng thất bại: {}", response.getMessage());
        }
    }
}