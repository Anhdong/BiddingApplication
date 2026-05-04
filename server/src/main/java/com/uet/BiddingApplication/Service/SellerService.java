package com.uet.BiddingApplication.Service;

import java.util.ArrayList;
import java.util.List;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
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

    private static volatile SellerService instance = null;

    private SellerService(){
        // TODO: Khởi tạo instance của ItemDAO và AuctionSessionDAO.
    }

    public static SellerService getInstance(){
        if (instance == null){
            synchronized (SellerService.class){
                if (instance == null){
                    instance = new SellerService();
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
     * Cập nhật thông tin vật phẩm (Chỉ hợp lệ khi phiên chưa OPEN).
     */
    public boolean updateItem(ItemUpdateRequestDTO request) {
        String itemId = request.getItemId();

        // 1. Lấy Entity từ DAO
        Item item = ItemDAO.getInstance().getItemById(itemId);
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại trong hệ thống.");
        }

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        // 2. Validate Business Logic: Chặn cập nhật nếu phiên không còn ở trạng thái chờ
        if (session != null && (session.getStatus() == SessionStatus.OPEN ||
                session.getStatus() == SessionStatus.RUNNING ||
                session.getStatus() == SessionStatus.FINISHED)) {
            throw new BusinessException("Không thể cập nhật vật phẩm khi phiên đấu giá đã mở, đang chạy hoặc đã kết thúc.");
        }

        // 3. Xử lý lưu trữ hình ảnh
        String imageURL = item.getImageURL(); // Giữ nguyên ảnh cũ nếu không có cập nhật
        if (request.getImageBytes() != null && request.getImageBytes().length > 0) {
            try {
                // Tải ảnh mới lên Supabase
                imageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());

                // Dọn dẹp rác: Xóa ảnh cũ trên storage để tiết kiệm dung lượng
                if (item.getImageURL() != null) {
                    StorageService.getInstance().deleteImage(item.getImageURL());
                }
            } catch (Exception e) {
                throw new BusinessException("Lỗi trong quá trình tải ảnh mới lên hệ thống lưu trữ.");
            }
        }

        // 4. Map dữ liệu từ DTO sang Entity
        Item updateItem = ItemMapper.toEntity(request, imageURL);

        // 5. Cập nhật Database và Đồng bộ RAM (Cache)
        boolean isUpdated = ItemDAO.getInstance().updateItem(updateItem);
        if (isUpdated) {
            SearchCacheManager.getInstance().updateItem(itemId, updateItem);
            return true;
        } else {
            throw new BusinessException("Lỗi hệ thống: Không thể ghi dữ liệu cập nhật xuống cơ sở dữ liệu.");
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

        // 2. Kiểm tra điều kiện ràng buộc phiên đấu giá
        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        // Theo đặc tả: Không được xóa nếu phiên đã mở, đang chạy hoặc đã kết thúc thành công
        if (session != null) {
            SessionStatus status = session.getStatus();
            if (status == SessionStatus.OPEN || status == SessionStatus.RUNNING || status == SessionStatus.FINISHED) {
                throw new BusinessException("Không thể xóa vật phẩm khi phiên đấu giá đang " + status.name() + ".");
            }
        }

        // 3. Dọn dẹp hình ảnh trên Storage (Supabase) trước khi xóa record
        if (item.getImageURL() != null && !item.getImageURL().isEmpty()) {
            try {
                StorageService.getInstance().deleteImage(item.getImageURL());
            } catch (Exception e) {
                // Lưu ý: Ta chỉ ghi log cảnh báo, không chặn luồng xóa chính nếu chỉ lỗi xóa file rác
                System.err.println("[Warning] Không thể dọn dẹp ảnh cũ trên Storage cho itemId: " + itemId);
            }
        }

        // 4. Thực hiện xóa dữ liệu dưới Database
        boolean isDeleted = ItemDAO.getInstance().deleteItem(itemId);

        if (isDeleted) {
            // 5. Cập nhật bộ nhớ đệm (Cache) để đồng bộ giao diện ngay lập tức
            SearchCacheManager.getInstance().removeItem(itemId);
            return true;
        } else {
            throw new BusinessException("Lỗi hệ thống: Không thể xóa vật phẩm khỏi cơ sở dữ liệu.");
        }
    }
    /**
     * Lấy danh sách các sản phẩm mà Seller đã đăng tải để hiển thị lên kho quản lý.
     */
    public List<Item> getItemsBySellerId(String sellerId) {
        // Kiểm tra đầu vào (Fail-fast validation)
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new BusinessException("Mã định danh người bán (Seller ID) không hợp lệ.");
        }

        // Gọi DAO để lấy danh sách Entity Item
        List<Item> items = ItemDAO.getInstance().getItemsBySellerId(sellerId);

        return items != null ? items : new ArrayList<>();
    }
}