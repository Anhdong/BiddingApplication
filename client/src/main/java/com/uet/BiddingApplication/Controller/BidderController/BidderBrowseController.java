package com.uet.BiddingApplication.Controller.BidderController;


import com.uet.BiddingApplication.Controller.BaseController.BaseBrowseController;
import com.uet.BiddingApplication.Controller.CommonController.ItemCardController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class BidderBrowseController extends BaseBrowseController {

    private static final Logger log = LoggerFactory.getLogger(BidderBrowseController.class);

    // Định nghĩa các Callback để có thể Subscribe/Unsubscribe chính xác
    private final Consumer<ResponsePacket<?>> auctionListCallback = this::handleAuctionListResponse;

    @Override
    protected void setupSubscriptions() {
        log.info("[BidderBrowse] Đăng ký lắng nghe các sự kiện đấu giá.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_ACTIVE_SESSIONS, auctionListCallback);
    }

    @Override
    protected void loadInitialData() {
        log.info("[BidderBrowse] Đang gửi yêu cầu lấy danh sách đấu giá...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_ACTIVE_SESSIONS);
        ServerConnection.getInstance().sendRequest(request);
    }

    @Override
    protected void unsubscribeAll() {
        log.info("[BidderBrowse] Hủy đăng ký lắng nghe sự kiện.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_ACTIVE_SESSIONS, auctionListCallback);
    }

    @Override
    protected void configureItem(ItemCardController controller, AuctionCardDTO item) {
        controller.setData(item);
        controller.setBtnAction("Add", event -> {
            log.info("[BidderBrowse] Bidder chọn đấu giá sản phẩm: {}", item.getItemName());
            // TODO: Chuyển sang màn hình chi tiết phiên đấu giá (Auction Detail)
        });
    }

    // --- XỬ LÝ DỮ LIỆU TỪ SERVER ---

    private void handleAuctionListResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            // Ép kiểu payload về danh sách DTO
            List<AuctionCardDTO> items = (List<AuctionCardDTO>) response.getPayload();
            // Lưu vào list gốc của Base để phục vụ Search Enter
            this.currentAuctions = items;

            // Render lên giao diện (BaseBrowse đã lo phần này)
            renderItems(items);

            log.info("[BidderBrowse] Đã load thành công {} sản phẩm.", items.size());
        } else {
            log.error("[BidderBrowse] Lỗi từ server: {}", response.getMessage());
        }
    }

}