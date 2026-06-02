package com.uet.BiddingApplication.Controller.SellerController;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
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

public class SellerHistoryController implements Initializable,ViewControllerLifecycle {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(SellerHistoryController.class);

    //--FXML--
    @FXML TableView<SellerHistoryResponseDTO> table;
    @FXML TableColumn<SellerHistoryResponseDTO,String> nameCol;
    @FXML TableColumn<SellerHistoryResponseDTO,BigDecimal> startPriceCol;
    @FXML TableColumn<SellerHistoryResponseDTO,BigDecimal> finalPriceCol;
    @FXML TableColumn<SellerHistoryResponseDTO,String> winnerCol;
    @FXML TableColumn<SellerHistoryResponseDTO, SessionStatus> statusCol;
    @FXML TableColumn<SellerHistoryResponseDTO, LocalDateTime> startTimeCol;
    @FXML TableColumn<SellerHistoryResponseDTO, LocalDateTime> endTimeCol;

    // Tạo ObservableList để quản lý dữ liệu
    private ObservableList<SellerHistoryResponseDTO> dataList = FXCollections.observableArrayList();

    //--CALLBACK--
    private final Consumer<ResponsePacket<?>> sellerHistoryCallback = this::handleSellerHistoryResponse;

    //--INIT/LIFECYCLE--

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        startPriceCol.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        finalPriceCol.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        winnerCol.setCellValueFactory(new PropertyValueFactory<>("winnerName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Liên kết danh sách dữ liệu với bảng
        table.setItems(dataList);
    }

    @Override
    public void onShow() {
        log.info("[SellerHistory] Đăng ký lắng nghe lịch sử vật phẩm.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_SELLER_HISTORY, sellerHistoryCallback);

        requestSellerHistory();
    }

    @Override
    public void onHide() {
        log.info("[SellerHistory] Hủy đăng ký lắng nghe lịch sử.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_SELLER_HISTORY, sellerHistoryCallback);
    }

    //--NETWORK REQUEST--
    private void requestSellerHistory() {
        log.info("[SellerHistory] Đang gửi yêu cầu lấy danh sách lịch sử vật phẩm...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_SELLER_HISTORY);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }
    //--HANDLE RESPONSE--
    private void handleSellerHistoryResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            if(response.getPayload() == null) return;
            dataList.clear();
            dataList.addAll((List<SellerHistoryResponseDTO>) response.getPayload());

            log.info("[SellerHistory] Lấy danh sách lịch sử thành công");
        } else {
            log.error("[SellerHistory] Lấy danh sách lịch sử thất bại: {}", response.getMessage());
        }
    }

//--MAIN METHOD--
public void updateTableSmartly(List<SellerHistoryResponseDTO> newList) {
    // Bước 1: XÓA những item cũ không còn xuất hiện trong danh sách mới
    dataList.removeIf(oldItem ->
            newList.stream().noneMatch(newItem -> newItem.getSessionId() == oldItem.getSessionId())
    );

    // Bước 2: THÊM HOẶC SỬA các item còn lại
    for (SellerHistoryResponseDTO newItem : newList) {
        boolean found = false;

        for (int i = 0; i < dataList.size(); i++) {
            SellerHistoryResponseDTO oldItem = dataList.get(i);

            // Tìm thấy item cũ trùng ID với item trong danh sách mới
            if (oldItem.getSessionId() == newItem.getSessionId()) {
                found = true;

                // Cập nhật lại các trường dữ liệu bằng setter thường
                //oldItem.setName(newItem.getName());
                //oldItem.setAge(newItem.getAge());

                // Ghi đè lại để ép TableView vẽ lại đúng dòng này nếu có thay đổi
                dataList.set(i, oldItem);
                break;
            }
        }

        // Nếu duyệt hết bảng mà không thấy ID này -> Đây là phần tử MỚI hoàn toàn
        if (!found) {
            dataList.add(newItem);
        }
    }
}





}
