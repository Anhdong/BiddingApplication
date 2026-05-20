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

    // Record đóng vai trò là một Tuple lưu cặp đối tượng (Node giao diện + Controller điều khiển)
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
        // Danh sách các tác vụ cập nhật dữ liệu của Card CŨ (phải chạy trên UI Thread)
        List<Runnable> updateUiTasks = new ArrayList<>();

        // Vòng lặp chạy trên Background Thread (do AppExecutor gọi) giúp giảm tải cho UI
        for (AuctionCardDTO item : items) {
            String sessionId = item.getSessionId();

            if (renderedCardsMap.containsKey(sessionId)) {
                // -------------------------------------------------------------
                // TRƯỜNG HỢP 1: CARD ĐÃ TỒN TẠI (Giữ nguyên, không load lại FXML)
                // -------------------------------------------------------------
                RenderedCard existingCard = renderedCardsMap.get(sessionId);

                // Vì Card cũ đang hiển thị trên màn hình, việc cập nhật text/button của nó
                // BẮT BUỘC phải đưa vào danh sách chờ để chạy trên JavaFX Application Thread sau.
                updateUiTasks.add(() -> {
                    existingCard.controller().setData(item);
                    configureItem(existingCard.controller(), item);
                });

                // Vẫn giữ lại Node cũ đưa vào danh sách hiển thị mới
                orderedNodes.add(existingCard.node());
                newMap.put(sessionId, existingCard);

            } else {
                // -------------------------------------------------------------
                // TRƯỜNG HỢP 2: CARD MỚI TOÀN BỘ (Thêm mới -> Load FXML)
                // -------------------------------------------------------------
                try {
                    // Việc nạp FXML diễn ra ở luồng nền (Background) giúp app không bị giật lag khi gõ tìm kiếm nhanh
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPath.ITEM_CARD.getPath()));
                    Node itemNode = loader.load();

                    ItemCardController itemController = loader.getController();

                    // Vì Node này chưa được đính (attach) vào Scene đang hiển thị công khai,
                    // việc gọi setData và configure ngay tại đây là hoàn toàn an toàn và mượt mà.
                    itemController.setData(item);
                    configureItem(itemController, item);

                    RenderedCard newCard = new RenderedCard(itemNode, itemController);
                    orderedNodes.add(itemNode);
                    newMap.put(sessionId, newCard);

                } catch (IOException e) {
                    log.error("[BaseBrowseController] Lỗi khi load ItemCardView.fxml cho sản phẩm {}", item.getItemName(), e);
                }
            }
        }

        // Đồng bộ map theo dõi ngay lập tức ở luồng nền để tránh race-condition nếu hàm này bị gọi dồn dập
        synchronized (renderedCardsMap) {
            renderedCardsMap.clear();
            renderedCardsMap.putAll(newMap);
        }

        // Đẩy toàn bộ thay đổi lên JavaFX Application Thread duy nhất 1 lần (Grouped Batch Update)
        Platform.runLater(() -> {
            // 1. Cập nhật dữ liệu mới cho các Card cũ đang giữ nguyên
            for (Runnable task : updateUiTasks) {
                task.run();
            }

            // 2. Đồng bộ danh sách con của TilePane.
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