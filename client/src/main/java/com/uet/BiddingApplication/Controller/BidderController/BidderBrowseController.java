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
import com.uet.BiddingApplication.Util.RegisteredSessionUtil;
import com.uet.BiddingApplication.Utils.PacketTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BidderBrowseController extends BaseBrowseController {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(BidderBrowseController.class);

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> auctionListCallback = this::handleAuctionListResponse;
    private final Consumer<ResponsePacket<?>> preRegisterCallback = this::handlePreRegisterResponse;

    private final Consumer<ResponsePacket<?>> registeredListCallback = this::handleRegisteredListResponse;

    //--DATA STORAGE FOR FILTERING--
    private List<AuctionCardDTO> rawActiveAuctions = new ArrayList<>();
    // Sử dụng Set để tối ưu hóa tốc độ tìm kiếm (.contains) khi lọc danh sách

    @Override
    protected void setupSubscriptions() {
        log.info("[BidderBrowse] Đăng ký lắng nghe các sự kiện đấu giá.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_ACTIVE_SESSIONS, auctionListCallback);
        ResponseDispatcher.getInstance().subscribe(ActionType.PRE_REGISTER_SESSION, preRegisterCallback);

        //Get response for when list is updated
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_REGISTERED_SESSIONS,registeredListCallback);

        // Gửi các yêu cầu lấy dữ liệu ban đầu
        requestActiveSessions();
        RegisteredSessionUtil.getInstance().requestRegisteredSessions();
    }

    @Override
    protected void unsubscribeAll() {
        log.info("[BidderBrowse] Hủy đăng ký lắng nghe sự kiện.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_ACTIVE_SESSIONS, auctionListCallback);
        ResponseDispatcher.getInstance().unsubscribe(ActionType.PRE_REGISTER_SESSION, preRegisterCallback);

        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_REGISTERED_SESSIONS,registeredListCallback);

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

        // Button Action (Chỉ hiện nút Đăng ký khi trạng thái OPEN hoặc RUNNING)
        if (status == SessionStatus.OPEN || status == SessionStatus.RUNNING) {
            controller.setButtonVisible(true);
            controller.setBtnAction("Add", event -> requestPreRegister(sessionId));
        } else {
            controller.setButtonVisible(false);
        }
    }

    // --- NETWORK REQUEST HELPERS ---

    private void requestActiveSessions() {
        log.info("[BidderBrowse] Đang gửi yêu cầu lấy danh sách đấu giá hoạt động...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_ACTIVE_SESSIONS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }
    private  void requestPreRegister(String sessionId){
        log.info("[BidderBrowse] Đang gửi yêu cầu đăng ký trước phiên: {}", sessionId);
        RequestPacket<SessionRegisterRequestDTO> request = new RequestPacket<>();
        request.setAction(ActionType.PRE_REGISTER_SESSION);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        request.setPayload(new SessionRegisterRequestDTO(sessionId));
        ServerConnection.getInstance().sendRequest(request);
    }

    // --- CORE FILTER & RENDER LOGIC ---

    private void filterPreRegister() {
        AppExecutor.execute(() -> {
            //Guard for even driven
            if (rawActiveAuctions == null || rawActiveAuctions.isEmpty()) {
                return;
            }

            log.info("[BidderBrowse] Bắt đầu lọc đăng kí từ danh sách nhận được");
            List<AuctionCardDTO> filteredList = rawActiveAuctions.stream()
                    .filter(auction -> !RegisteredSessionUtil.getInstance().isRegistered(auction.getSessionId()))
                    .toList();

            // Add to currentAuctions for search
            this.currentAuctions = filteredList;

            renderItems(filteredList);
        });
    }

    // --- HANDLE RESPONSE ---

    private void handleAuctionListResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            this.rawActiveAuctions = (List<AuctionCardDTO>) response.getPayload();
            log.info("[BidderBrowse] Nhận được {} phiên đấu giá hoạt động từ server.", rawActiveAuctions.size());

            // Kích hoạt tính toán lại bộ lọc công khai
            filterPreRegister();
        } else {
            log.error("[BidderBrowse] Lỗi lấy danh sách từ server: {}", response.getMessage());
        }
    }


    private void handlePreRegisterResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            NotificationUtil.showInfo("Thành công", "Đăng ký tham gia đấu giá thành công!");
            log.info("[BidderBrowse] Đăng ký sản phẩm thành công. Đang cập nhật lại danh sách...");

            RegisteredSessionUtil.getInstance().requestRegisteredSessions();
            requestActiveSessions();

        } else {
            NotificationUtil.showError("Thất bại", response.getMessage());
            log.error("[BidderBrowse] Đăng ký sản phẩm không thành công: {}", response.getMessage());
        }
    }

    private void handleRegisteredListResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            log.info("[BidderBrowse] RegisteredSessionUtil list updated");
            filterPreRegister();
        } else {
            log.error("[BidderBrowse] RegisteredSessionUtil list could not update: {}", response.getMessage());
        }
    }
}