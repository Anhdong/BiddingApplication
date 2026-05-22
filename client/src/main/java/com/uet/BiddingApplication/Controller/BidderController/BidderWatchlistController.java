package com.uet.BiddingApplication.Controller.BidderController;

import com.uet.BiddingApplication.Controller.BaseController.BaseBrowseController;
import com.uet.BiddingApplication.Controller.CommonController.ItemCardController;
import com.uet.BiddingApplication.Controller.CommonController.ItemDetailController;
import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.SessionRegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import com.uet.BiddingApplication.Util.AppExecutor;
import com.uet.BiddingApplication.Util.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BidderWatchlistController extends BaseBrowseController {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(BidderWatchlistController.class);

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> registeredListCallback = this::handleRegisteredListResponse;
    private final Consumer<ResponsePacket<?>> delRegisterCallback = this::handleDelRegisterResponse;

    @Override
    protected void setupSubscriptions() {
        log.info("[BidderWatchlist] Đăng ký lắng nghe các vật phẩm đã đăng kí.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_REGISTERED_SESSIONS, registeredListCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.DELETE_REGISTER_SESSION, delRegisterCallback);

        // Gửi các yêu cầu lấy dữ liệu ban đầu
        requestRegisteredSessions();
    }

    @Override
    protected void unsubscribeAll() {
        log.info("[BidderWatchlist] Hủy đăng ký lắng nghe sự kiện.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_REGISTERED_SESSIONS, registeredListCallback);
        ResponseDispatcher.getInstance().unsubscribe(ActionType.DELETE_REGISTER_SESSION, delRegisterCallback);
    }

    @Override
    protected void configureItem(ItemCardController controller, AuctionCardDTO item) {
        SessionStatus status = item.getStatus();
        String sessionId = item.getSessionId();

        // Card Action
        if (Objects.requireNonNull(status) == SessionStatus.OPEN) {
            controller.setCardAction(event -> MainViewController.getInstance().loadView(ViewPath.ITEM_DETAIL,
                    (ItemDetailController c) -> c.setCurrentSessionID(sessionId)));
        } //TODO: add logic to set up auction room khi STATUS == RUNNING

        // Button Action (Chỉ hiện nút Hủy đăng ký khi trạng thái OPEN hoặc RUNNING)
        if (status == SessionStatus.OPEN || status == SessionStatus.RUNNING) {
            controller.setButtonVisible(true);
            controller.setBtnAction("Remove", event -> requestDeleteRegister(sessionId));
        } else {
            controller.setButtonVisible(false);
        }
    }

    // --- NETWORK REQUEST HELPERS ---

    private void requestDeleteRegister(String sessionId) {
        log.info("[BidderWatchlist] Đang gửi yêu cầu hủy đăng kí trc phiên: {}",sessionId);
        RequestPacket<SessionRegisterRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.DELETE_REGISTER_SESSION);
        request.setPayload(new SessionRegisterRequestDTO(sessionId));
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }

    private void requestRegisteredSessions() {
        log.info("[BidderWatchlist] Đang gửi yêu cầu lấy danh sách đấu giá đã đăng ký...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_REGISTERED_SESSIONS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }

    // --- HANDLE RESPONSE ---

    private void handleRegisteredListResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            AppExecutor.execute(() -> {
                List<AuctionCardDTO> items = (List<AuctionCardDTO>) response.getPayload();
                log.info("[BidderWatchlist] Nhận được {} phiên đấu giá hoạt động từ server.", items.size());

                // Lưu vào danh sách gốc của lớp cha để phục vụ tính năng tìm kiếm (onSearchEnter) không bị lỗi dữ liệu
                this.currentAuctions = items;

                // Gọi hàm render đồng bộ thông minh của BaseBrowseController
                renderItems(items);
            });
        } else {
            log.error("[BidderWatchlist] Lỗi lấy danh sách từ server: {}", response.getMessage());
        }
    }


    private void handleDelRegisterResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Thành công", "Hủy đăng ký tham gia đấu giá thành công!");
            AppExecutor.execute(()->{
                log.info("[BidderWatchlist] Hủy đăng ký sản phẩm thành công. Đang cập nhật lại danh sách...");
                //Gọi lại danh sách để làm mới view
                requestRegisteredSessions();
            });
        } else {
            NotificationUtil.showError("Thất bại", response.getMessage());
            log.error("[BidderWatchlist] Hủy đăng ký sản phẩm không thành công: {}", response.getMessage());
        }
    }
}