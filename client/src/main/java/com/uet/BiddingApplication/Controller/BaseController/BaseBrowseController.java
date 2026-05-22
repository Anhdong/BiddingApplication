package com.uet.BiddingApplication.Controller.BaseController;

import com.uet.BiddingApplication.Controller.CommonController.ItemCardController;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ViewPath;
import com.uet.BiddingApplication.Interface.ViewControllerLifecycle;
import com.uet.BiddingApplication.Util.AppExecutor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseBrowseController implements ViewControllerLifecycle {

    //--LOG--
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseBrowseController.class);

    // --FXML--
    @FXML protected TilePane itemContainer;
    @FXML protected TextField searchField;
    protected String currentSearchKeyword = "";

    //--LIST & MAP OPTIMIZATION--
    protected List<AuctionCardDTO> currentAuctions = new ArrayList<>();

    // Record là Tuple lưu cặp đối tượng để làm value trong Map
    protected record RenderedCard(Node node, ItemCardController controller) {}

    // Map quản lý các card đang hiển thị trên màn hình (Key: SessionID, Value: RenderedCard)
    protected final Map<String, RenderedCard> renderedCardsMap = new HashMap<>();

    //--LIFE CYCLE--
    @Override
    public void onShow() { setupSubscriptions(); }

    @Override
    public void onHide() { unsubscribeAll(); }

    //--METHODS--
    protected void renderItems(List<AuctionCardDTO> items) {
        log.info("Bắt đầu cập nhật cây giao diện tối ưu. Số lượng: {}", items.size());

        // Danh sách lưu đúng thứ tự các Node sẽ hiển thị sau khi lọc/sắp xếp
        List<Node> orderedNodes = new ArrayList<>();
        // Map tạm thời để lưu trạng thái mới sau khi xử lý xong xuôi
        Map<String, RenderedCard> newMap = new HashMap<>();
        // Danh sách các tác vụ cập nhật dữ liệu của Card CŨ (UI Thread)
        List<Runnable> updateUiTasks = new ArrayList<>();

        for (AuctionCardDTO item : items) {
            String sessionId = item.getSessionId();

            if (renderedCardsMap.containsKey(sessionId)) {
                RenderedCard existingCard = renderedCardsMap.get(sessionId);

                //Add update existing card to taskList
                updateUiTasks.add(() -> {
                    existingCard.controller().setData(item);
                    configureItem(existingCard.controller(), item);
                });

                // ReAdd already load node to new list
                orderedNodes.add(existingCard.node());
                newMap.put(sessionId, existingCard);

            } else {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPath.ITEM_CARD.getPath()));
                    Node itemNode = loader.load();
                    ItemCardController itemController = loader.getController();

                    //View not on scene yet so can setData right here
                    itemController.setData(item);
                    configureItem(itemController, item);

                    //Add to new list
                    RenderedCard newCard = new RenderedCard(itemNode, itemController);
                    orderedNodes.add(itemNode);
                    newMap.put(sessionId, newCard);

                } catch (IOException e) {
                    log.error("[BaseBrowseController] Lỗi khi load ItemCardView.fxml cho sản phẩm {}", item.getItemName(), e);
                }
            }
        }

        // Update in backgroundThread (Synchronoized to prevent race-condition)
        synchronized (renderedCardsMap) {
            renderedCardsMap.clear();
            renderedCardsMap.putAll(newMap);
        }

        // Run task in FxThread (Grouped Batch Update)
        Platform.runLater(() -> {
            for (Runnable task : updateUiTasks) {
                task.run();
            }

            // setAll sẽ tự động thêm các node mới, xóa các node thiếu, và không đụng vào các node giữ nguyên.
            itemContainer.getChildren().setAll(orderedNodes);
        });
    }

    @FXML
    public void onSearchEnter() {
        if (searchField != null) {
            currentSearchKeyword = searchField.getText().trim().toLowerCase();
            AppExecutor.execute(() -> {
                // Filter list
                List<AuctionCardDTO> filteredList = currentAuctions.stream()
                        .filter(item -> currentSearchKeyword.isEmpty() ||
                                item.getItemName().toLowerCase().contains(currentSearchKeyword))
                        .toList();
                // Render một cách tối ưu
                renderItems(filteredList);

                log.info("Đang tìm kiếm với từ khóa: {}", currentSearchKeyword);
            });
        }
    }

    // =========================================================================
    // ABSTRACT FUNCTION
    // =========================================================================
    protected abstract void setupSubscriptions();
    protected abstract void unsubscribeAll();
    protected abstract void configureItem(ItemCardController controller, AuctionCardDTO item);
}