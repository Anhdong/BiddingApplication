package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.DTO.Request.RelistRequestDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.AuctionSessionMapper;
import com.uet.BiddingApplication.Utils.Mapper.ItemMapper;
import com.uet.BiddingApplication.Utils.StorageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp quản lý nghiệp vụ tạo mới và tái đăng bán sản phẩm.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class ItemManagementService {

    private ItemDAO itemDAO;
    private AuctionSessionDAO sessionDAO;

    private static volatile ItemManagementService instance = null;

    private ItemManagementService(){
        this.itemDAO = ItemDAO.getInstance();
        this.sessionDAO = AuctionSessionDAO.getInstance();
    }

    public static ItemManagementService getInstance(){
        if (instance == null){
            synchronized (ItemManagementService.class) {
                if (instance == null) {
                    instance = new ItemManagementService();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo sản phẩm mới và mở phiên đấu giá cho sản phẩm đó.
     * @return true nếu mọi bước thực hiện thành công, ném Exception nếu có lỗi nghiệp vụ.
     */
    public boolean createItemAndOpenSession(ItemCreateDTO request, String sellerId) {
        // 1. Kiểm tra đầu vào cơ bản (Fail-fast)
        if (request == null || sellerId == null) {
            throw new BusinessException("Dữ liệu yêu cầu không hợp lệ."); // [cite: 687]
        }

        // 2. Xử lý lưu trữ hình ảnh qua StorageService
        String imageURL;
        try {
            // Nhận byte[] từ DTO và tải lên hệ thống lưu trữ [cite: 849, 1027]
            imageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());
        } catch (Exception e) {
            // Nếu lỗi upload ảnh, chặn quy trình và báo lỗi cụ thể
            throw new BusinessException("Không thể tải lên hình ảnh sản phẩm. Vui lòng thử lại.");
        }

        // 3. Chuyển đổi DTO sang Entity Item thông qua Mapper [cite: 1091]
        Item newItem = ItemMapper.toEntity(request, sellerId, imageURL);

        // 4. Lưu vật phẩm vào Database thông qua ItemDAO [cite: 1041, 1128]
        if (!itemDAO.insertItem(newItem)) {
            throw new BusinessException("Lỗi hệ thống: Không thể khởi tạo thông tin vật phẩm.");
        }

        // 5. Khởi tạo và lưu phiên đấu giá (AuctionSession) liên kết với Item vừa tạo [cite: 1041, 1093, 1135]
        AuctionSession newSession = AuctionSessionMapper.toEntity(request, newItem.getId());
        if (!sessionDAO.insertSession(newSession)) {
            // Nếu bước này lỗi, lý tưởng nhất là có cơ chế rollback xóa Item đã tạo ở trên
            throw new BusinessException("Lỗi hệ thống: Không thể mở phiên đấu giá cho sản phẩm này.");
        }

        // 6. Cập nhật dữ liệu nóng lên RAM (Write-through Cache) [cite: 1041, 1162]
        // Việc này đảm bảo các Bidder khác thấy ngay sản phẩm mới mà không cần chạm DB [cite: 1153, 1175]
        SearchCacheManager.getInstance().addSessionAndItem(newSession, newItem);

        // Trả về true nếu toàn bộ quy trình hoàn tất không có lỗi [cite: 674]
        return true;
    }

    /**
     * Cập nhật thông tin phiên đấu giá (nếu đang OPEN)
     * hoặc mở phiên đấu giá mới/đăng bán lại (nếu đã FINISHED/CANCELED).
     * @return true nếu thao tác thành công.
     */
    public boolean relistUnsoldItem(RelistRequestDTO request, String sellerId) {
        // 1. Kiểm tra tính hợp lệ của DTO (Fail-fast)
        if (request == null || request.getItemId() == null || request.getSessionId() == null) {
            throw new BusinessException("Dữ liệu yêu cầu không hợp lệ.");
        }

        // BỔ SUNG: Chặn đặt thời gian trong quá khứ
        if (request.getNewStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Thời gian bắt đầu không thể diễn ra trong quá khứ.");
        }

        if (request.getNewStartTime().isAfter(request.getNewEndTime())) {
            throw new BusinessException("Thời gian bắt đầu không thể sau thời gian kết thúc.");
        }

        if (request.getNewStartPrice() == null || request.getNewStartPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá khởi điểm phải lớn hơn 0.");
        }

        // 2. Kiểm tra sự tồn tại và quyền sở hữu vật phẩm
        Item item = itemDAO.getItemById(request.getItemId());
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại.");
        }

        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessException("Bạn không có quyền thao tác với vật phẩm này.");
        }

        // 3. Lấy thông tin phiên đấu giá cũ
        AuctionSession oldSession = sessionDAO.getSessionById(request.getSessionId());
        if (oldSession == null) {
            throw new BusinessException("Phiên đấu giá không tồn tại.");
        }

        // 4. Định tuyến logic dựa trên Trạng thái (Status) của phiên
        SessionStatus currentStatus = oldSession.getStatus();

        if (currentStatus == SessionStatus.OPEN) {
            /* TRƯỜNG HỢP 1: PHIÊN CHƯA BẮT ĐẦU -> CẬP NHẬT TRỰC TIẾP */
            oldSession.setStartPrice(request.getNewStartPrice());
            oldSession.setStartTime(request.getNewStartTime());
            oldSession.setEndTime(request.getNewEndTime());

            if (!sessionDAO.updateSession(oldSession)) {
                throw new BusinessException("Lỗi hệ thống: Không thể cập nhật thông tin phiên đấu giá.");
            }

            SearchCacheManager.getInstance().addSessionAndItem(oldSession, item);
            return true;

        } else if (currentStatus == SessionStatus.FINISHED || currentStatus == SessionStatus.CANCELED) {
            /* TRƯỜNG HỢP 2: PHIÊN ĐÃ KẾT THÚC/HỦY -> TẠO PHIÊN MỚI (ĐĂNG LẠI) */
            AuctionSession newSession = AuctionSessionMapper.toEntity(request);
            newSession.setItemId(item.getId());

            if (!sessionDAO.insertSession(newSession)) {
                throw new BusinessException("Lỗi hệ thống: Không thể mở lại phiên đấu giá mới.");
            }

            SearchCacheManager.getInstance().addSessionAndItem(newSession, item);
            return true;

        } else {
            /* TRƯỜNG HỢP 3: PHIÊN ĐANG CHẠY (RUNNING) -> CẤM CAN THIỆP */
            throw new BusinessException("Không thể chỉnh sửa hoặc đăng lại khi phiên đấu giá đang diễn ra.");
        }
    }
}