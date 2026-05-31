package com.uet.BiddingApplication.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.DTO.Request.ItemUpdateRequestDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.AuctionSessionMapper;
import com.uet.BiddingApplication.Utils.Mapper.ItemMapper;
import com.uet.BiddingApplication.Utils.StorageService;

/**
 * Lớp nghiệp vụ dành cho các chức năng của Người bán (Seller).
 * Áp dụng mẫu thiết kế Singleton.
 */
public class SellerService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellerService.class);

    private static volatile SellerService instance = null;

    private SellerService(){
    }

    public static SellerService getInstance(){
        if (instance == null){
            synchronized (SellerService.class){
                if (instance == null){
                    instance = new SellerService();
                // Nếu request.getBidStep() == null, code bỏ qua nhánh này, session giữ nguyên bidStep cũ
                }
            }
        }
        return instance;
    }

    /**
     * Lấy thống kê lịch sử các mặt hàng đã kết thúc của Seller.
     */
    public List<SellerHistoryResponseDTO> getSellerHistory(String sellerId) {
        // 1. Kiểm tra tính hợp lệ của đầu vào
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new BusinessException("Mã định danh người bán (Seller ID) không được để trống.");
        }

        // 2 & 3. Gọi DAO (DAO đã lo phần lọc trạng thái và đóng gói DTO)
        return AuctionSessionDAO.getInstance().getSellerHistory(sellerId);
    }

    /**
     * Cập nhật thông tin vật phẩm.
     * Hợp lệ khi phiên chưa được tạo (null) hoặc đang ở trạng thái chờ/OPEN.
     */
    public boolean updateItem(ItemUpdateRequestDTO request) {
        String itemId = request.getItemId();

        // 1. Lấy Entity từ DAO
        Item item = ItemDAO.getInstance().getItemById(itemId);
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại trong hệ thống.");
        }

        // 2. Kiểm tra điều kiện ràng buộc phiên đấu giá
        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        // 2. Validate Business Logic: Chặn cập nhật nếu phiên đang chạy hoặc đã kết thúc
        if (session != null && (session.getStatus() == SessionStatus.RUNNING ||
                session.getStatus() == SessionStatus.FINISHED)) {
            throw new BusinessException("Không thể cập nhật vật phẩm khi phiên đấu giá đang " + session.getStatus().name() + ".");
        }

        // 3. Xử lý lưu trữ hình ảnh (Upload ảnh mới trước, giữ nguyên ảnh cũ)
        String newImageURL = item.getImageURL();
        boolean hasNewImage = false;

        if (request.getImageBytes() != null && request.getImageBytes().length > 0) {
            try {
                // CHÚ Ý: Tải ảnh mới lên, nhưng CHƯA xóa ảnh cũ ở bước này
                newImageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());
                hasNewImage = true;
            } catch (Exception e) {
                log.error("Lỗi upload ảnh mới cho itemId: " + itemId, e);
                throw new BusinessException("Lỗi trong quá trình tải ảnh mới lên hệ thống lưu trữ.");
            }
        }

        // 4. Map dữ liệu từ DTO sang Entity
        Item updateItem = ItemMapper.toEntity(request, newImageURL);

        // BỌC NULL CHECK: Tránh NullPointerException nếu Item chưa có Session
        if (session != null) {
            // Cập nhật StartTime và EndTime (nên có null-check nếu form update cho phép bỏ trống)
            if (request.getStartTime() != null) session.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) session.setEndTime(request.getEndTime());

            // Xử lý StartPrice
            if (request.getStartPrice() != null) {
                if (request.getStartPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Giá khởi điểm phải lớn hơn 0.");
                }
                session.setStartPrice(request.getStartPrice());
            }

            // XỬ LÝ BIDSTEP
            if (request.getBidStep() != null) {
                if (request.getBidStep().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Bước giá phải lớn hơn 0.");
                }
                session.setBidStep(request.getBidStep());
            }
        }

        // 5. Cập nhật Database
        // LƯU Ý KIẾN TRÚC: Lý tưởng nhất là 2 hàm update này nằm trong 1 Database Transaction ở tầng DAO.
        // Ở đây xử lý tuần tự để đảm bảo logic hiện tại của bạn không bị crash.
        boolean isItemUpdated = ItemDAO.getInstance().updateItem(updateItem);
        boolean isSessionUpdated = true; // Mặc định true nếu không có session để update

        if (isItemUpdated && session != null) {
            isSessionUpdated = AuctionSessionDAO.getInstance().updateSession(session);
        }

        // 6. Xử lý kết quả & Đồng bộ
        System.out.println(isItemUpdated + " " + isSessionUpdated);
        if (isItemUpdated && isSessionUpdated) {
            // 6.1 Đồng bộ Cache
            SearchCacheManager.getInstance().updateItem(itemId, updateItem);

            if (session != null) {
                SearchCacheManager.getInstance().updateSession(session.getId(), session);
                // CẬP NHẬT SCHEDULER: Đảm bảo trigger chạy phiên kích hoạt đúng giờ mới
                SessionStartScheduler.getInstance().cancelSchedule(session.getId());
                SessionStartScheduler.getInstance().scheduleStart(session.getId(),session.getStartTime());
            }

            // 6.2 Dọn dẹp rác (Xóa ảnh cũ SAU KHI Database đã commit thành công)
            if (hasNewImage && item.getImageURL() != null && !item.getImageURL().isEmpty()) {
                try {
                    StorageService.getInstance().deleteImage(item.getImageURL());
                } catch (Exception e) {
                    // Lỗi xóa rác không nên ném ra Exception làm gián đoạn luồng thành công
                    log.warn("[Warning] Lỗi khi dọn dẹp ảnh cũ trên Storage cho itemId: " + itemId, e);
                }
            }
            return true;
        } else {
            // ROLLBACK STORAGE: Nếu update DB thất bại nhưng đã upload ảnh mới, cần xóa ảnh vừa upload để tránh rác
            if (hasNewImage) {
                try {
                    StorageService.getInstance().deleteImage(newImageURL);
                } catch (Exception e) {
                    log.error("[Error] Không thể rollback ảnh mới trên Storage khi DB update lỗi.", e);
                }
            }
            throw new BusinessException("Lỗi hệ thống: Không thể đồng bộ dữ liệu cập nhật xuống cơ sở dữ liệu.");
        }
    }

    /**
     * Xóa vật phẩm khỏi hệ thống.
     * Áp dụng logic kiểm tra phiên và dọn dẹp tài nguyên lưu trữ.
     */
    public boolean deleteItem(String itemId) {
        // 1. Kiểm tra vật phẩm có tồn tại hay không
        Item item = ItemDAO.getInstance().getItemById(itemId);
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại hoặc đã bị xóa trước đó.");
        }

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        // Theo đặc tả: Không được xóa nếu phiên đã mở, đang chạy hoặc đã kết thúc thành công
        if (session != null) {
            SessionStatus status = session.getStatus();
            if (status == SessionStatus.RUNNING || status == SessionStatus.FINISHED) {
                throw new BusinessException("Không thể xóa vật phẩm khi phiên đấu giá đang " + status.name() + ".");
            }
        }

        // 3. Dọn dẹp hình ảnh trên Storage (Supabase) trước khi xóa record
        if (item.getImageURL() != null && !item.getImageURL().isEmpty()) {
            try {
                StorageService.getInstance().deleteImage(item.getImageURL());
            } catch (Exception e) {
                // Lưu ý: Ta chỉ ghi log cảnh báo, không chặn luồng xóa chính nếu chỉ lỗi xóa file rác
                log.error("[Warning] Không thể dọn dẹp ảnh cũ trên Storage cho itemId: " + itemId);
            }
        }

        // 4. Thực hiện xóa dữ liệu dưới Database
        boolean isDeleted = ItemDAO.getInstance().deleteItem(itemId);

        if (isDeleted) {
            // 5. Cập nhật bộ nhớ đệm (Cache) để đồng bộ giao diện ngay lập tức
            SearchCacheManager.getInstance().removeItem(itemId);
            if (session != null) {
                SearchCacheManager.getInstance().removeSession(session.getId());
                SessionStartScheduler.getInstance().cancelSchedule(session.getId());
            }
            return true;
        } else {
            throw new BusinessException("Lỗi hệ thống: Không thể xóa vật phẩm khỏi cơ sở dữ liệu.");
        }
    }
    /**
     * Lấy danh sách các sản phẩm mà Seller đã đăng tải để hiển thị lên kho quản lý.
     */
    public List<AuctionCardDTO> getItemsBySellerId(String sellerId) {
        // Kiểm tra đầu vào (Fail-fast validation)
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new BusinessException("Mã định danh người bán (Seller ID) không hợp lệ.");
        }

        // Gọi DAO để lấy danh sách Entity Item
        List<AuctionCardDTO> items = ItemDAO.getInstance().getSellerItems(sellerId);

        return items != null ? items : new ArrayList<>();
    }
}