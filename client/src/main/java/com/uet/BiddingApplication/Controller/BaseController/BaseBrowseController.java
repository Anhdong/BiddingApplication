package com.uet.BiddingApplication.Controller.BaseController;


import com.uet.BiddingApplication.Controller.CommonController.ItemCardController;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Util.AppExecutor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public abstract class BaseBrowseController implements Initializable, ViewControllerLifecycle {

    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseBrowseController.class);

    // --FXML FIELDS--
    @FXML protected TilePane itemContainer;
    @FXML protected TextField searchField;
    protected String currentSearchKeyword = "";

    //--LIST--
    //help update single items without render all again
    protected List<AuctionCardDTO> currentAuctions = new ArrayList<>();

    //--INITIAL--
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadInitialData();    // Sau đó mới gửi request lấy dữ liệu
    }
    //--LifeCycle--
    @Override
    public void onShow() {setupSubscriptions();}

    @Override
    public void onHide() {unsubscribeAll();}

    //--METHODS--
    protected void renderItems(List<AuctionCardDTO> items) {
            log.info("Clean all items");
            Platform.runLater(()->{itemContainer.getChildren().clear();}); // Clean all old cards
            log.info("Load all Item cards");
            for (AuctionCardDTO item : items) {
                try {
                    // Load Card
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPath.ITEM_CARD.getPath()));
                    Node itemNode = loader.load();

                    // Get ItemCardController and setData
                    ItemCardController itemController = loader.getController();
                    itemController.setData(item);

                    // Config button action base on Browse
                    configureItem(itemController, item);

                    // Add Card to Container
                    Platform.runLater(()->{itemContainer.getChildren().add(itemNode);});

                } catch (IOException e) {
                    log.error("[BaseBrowseController] Lỗi khi load ItemCardView.fxml cho sản phẩm {}", item.getItemName());
                }
            }
    }

    @FXML
    public void onSearchEnter() {
        if (searchField != null) {
            // getText when pressing Enter
            currentSearchKeyword = searchField.getText().trim().toLowerCase();
            AppExecutor.execute(()->{
                //Filter list
                List<AuctionCardDTO> filteredList = currentAuctions.stream()
                        .filter(item -> currentSearchKeyword.isEmpty() ||
                                item.getItemName().toLowerCase().contains(currentSearchKeyword))
                        .toList();
                //Render
                renderItems(filteredList);

                log.info("Đang tìm kiếm với từ khóa: {}", currentSearchKeyword);
            });
        }
    }
    //Sau nếu apply filter thì có thể tách riêng hàm applyFilter

    // =========================================================================
    // ABSTRACT FUNCTION
    // =========================================================================

    /**
     * Dùng để đăng ký các luồng nhận dữ liệu từ ResponseDispatcher
     */
    protected abstract void setupSubscriptions();

    /**
     * Dùng để gửi RequestPacket lên Server (Ví dụ: GET_ALL_AUCTIONS hay GET_MY_AUCTIONS)
     */
    protected abstract void loadInitialData();

    /**
     * Dùng để dọn dẹp các đăng ký mạng khi người dùng chuyển sang giao diện khác
     */
    protected abstract void unsubscribeAll();

    //Extended browse need to config the label and button action
    protected abstract void configureItem(ItemCardController controller, AuctionCardDTO item);
}