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
import com.uet.BiddingApplication.Util.AppExecutor;
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
    @FXML private Button btnAdd;

    //--CALLBACKS--
    private final Consumer<ResponsePacket<?>> itemsListCallback = this::handleItemsListResponse;

    @Override
    public void onShow() {
        log.info("[SellerBrowse] Đăng ký lắng nghe các vật phẩm đăng bán.");
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_SELLER_ITEMS, itemsListCallback);

        requestSellerItems();
    }

    @Override
    public void onHide() {
        log.info("[SellerBrowse] Hủy đăng ký lắng nghe sự kiện.");
        ResponseDispatcher.getInstance().unsubscribe(ActionType.GET_SELLER_ITEMS, itemsListCallback);
    }

    @Override
    protected void configureItem(ItemCardController controller, AuctionCardDTO item) {
        controller.setButtonVisible(true);
        controller.setBtnAction("Edit", event -> {
            log.info("[SellerBrowse] Seller chọn chỉnh sửa sản phẩm: {}", item.getItemName());
            MainViewController.getInstance().loadView(ViewPath.SELLER_ITEM_FORM, (SellerItemsFormController c) -> {
                c.setupFormMode(item.getSessionId(), item.getItemId());
            });
        });

    }

    // --- NETWORK REQUEST HELPERS ---

    private void requestSellerItems() {
        log.info("[SellerBrowse] Đang gửi yêu cầu lấy danh sách vật phẩm đăng bán...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_SELLER_ITEMS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }

    // --- HANDLE RESPONSE ---

    private void handleItemsListResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            AppExecutor.execute(() -> {
                List<AuctionCardDTO> items = (List<AuctionCardDTO>) response.getPayload();

                // Lưu vào danh sách gốc của lớp cha để phục vụ tính năng tìm kiếm (onSearchEnter) không bị lỗi dữ liệu
                this.currentAuctions = items;

                // Gọi hàm render đồng bộ thông minh của BaseBrowseController
                renderItems(items);

                log.info("[SellerBrowse] Đã đồng bộ thành công {} sản phẩm lên màn hình hiển thị.", items.size());
            });
        } else {
            log.error("[SellerBrowse] Lỗi từ server khi tải danh sách vật phẩm: {}", response.getMessage());
        }
    }

    //--ADD BUTTON ACTION--
    @FXML
    private void switchToAddItem() {
        MainViewController.getInstance().loadView(ViewPath.SELLER_ITEM_FORM, (SellerItemsFormController c) -> {
            c.setupFormMode(null,null); // Mode tạo mới sản phẩm (truyền null)
        });
    }
}