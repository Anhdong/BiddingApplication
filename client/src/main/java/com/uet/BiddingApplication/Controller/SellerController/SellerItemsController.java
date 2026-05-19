package com.uet.BiddingApplication.Controller.SellerController;


import com.uet.BiddingApplication.Controller.BaseController.BaseBrowseController;
import com.uet.BiddingApplication.Controller.CommonController.ItemCardController;
import com.uet.BiddingApplication.Controller.MainViewController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class SellerItemsController extends BaseBrowseController {
    //--LOG--
    private static final Logger log = LoggerFactory.getLogger(SellerItemsController.class);

    //--FXML--
    @FXML Button btnAdd;


    // Định nghĩa các Callback để có thể Subscribe/Unsubscribe chính xác
    private final Consumer<ResponsePacket<?>> ItemsListCallback = this::handleItemsListResponse;

    @Override
    protected void setupSubscriptions() {
        log.info("[SellerBrowse] Đăng ký lắng nghe các vật phẩm đăng bán.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_SELLER_ITEMS, ItemsListCallback);

        log.info("[SellerBrowse] Đang gửi yêu cầu lấy danh sách vật phẩm đăng bán...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_SELLER_ITEMS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }

    @Override
    protected void unsubscribeAll() {
        log.info("[SellerBrowse] Hủy đăng ký lắng nghe sự kiện.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_SELLER_ITEMS, ItemsListCallback);
    }

    @Override
    protected void configureItem(ItemCardController controller, AuctionCardDTO item) {
        controller.setBtnAction("Edit", event -> {
            log.info("[SellerBrowse] Bidder chọn chỉnh sửa sản phẩm: {}", item.getItemName());
            // TODO: Chuyển sang màn hình chi tiết phiên đấu giá (Auction Detail)
        });
    }

    // --- HANDLE RESPONSE ---

    private void handleItemsListResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            // Ép kiểu payload về danh sách DTO
            List<AuctionCardDTO> items = (List<AuctionCardDTO>) response.getPayload();
            // Lưu vào list gốc của Base để phục vụ Search Enter
            this.currentAuctions = items;

            // Render lên giao diện (BaseBrowse đã lo phần này)
            renderItems(items);

            log.info("[SellerBrowse] Đã load thành công {} sản phẩm.", items.size());
        } else {
            log.error("[SellerBrowse] Lỗi từ server: {}", response.getMessage());
        }
    }

    //--ADD BUTTON--
    @FXML
    private void switchToAddItem(){
        MainViewController.getInstance().loadView(ViewPath.SELLER_ITEM_FORM,(SellerItemsFormController c)->{
            c.setupFormMode(null);
        });
    }
}