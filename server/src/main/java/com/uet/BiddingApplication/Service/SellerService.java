package com.uet.BiddingApplication.Service;

import java.util.List;

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
    public List<SellerHistoryResponseDTO> getSellerHistory(String sellerId){
        // TODO 1 (Input): Nhận sellerId.
        // TODO 2 (Dependencies): Gọi AuctionSessionDAO để lấy các phiên (chỉ FINISHED hoặc CANCELED).
        // TODO 3 (Processing/Output): Map dữ liệu và trả về danh sách SellerHistoryResponseDTO.
        return AuctionSessionDAO.getInstance().getSellerHistory(sellerId);
    }

    /**
     * Cập nhật thông tin vật phẩm (Chỉ hợp lệ khi phiên chưa bắt đầu).
     */
    public void updateItem(ItemUpdateRequestDTO request, String itemId){
        // TODO 1 (Input): Nhận thông tin cần sửa từ request.
        // TODO 2 (Processing): Kiểm tra trạng thái phiên, nếu đang RUNNING thì chặn ném Exception.
        // TODO 3 (Side-effect): Gọi ItemDAO.updateItem(...) để ghi xuống cơ sở dữ liệu.

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        if (session.getStatus() == SessionStatus.RUNNING){
            throw new BusinessException("Phiên đang được đấu giá không được cập nhật item.");
        }

        String imageURL;

        if (request.getImageBytes() == null && request.getImageExtension() == null){
            imageURL = null;
        } else {
            try {
                imageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());
            } catch (Exception e) {
                throw new BusinessException("Lỗi upload ảnh.");
            }
        }

        Item updateItem = ItemMapper.toEntity(request, imageURL);
        ItemDAO.getInstance().updateItem(updateItem);
    }

    /**
     * Xóa vật phẩm khỏi hệ thống.
     */
    public void deleteItem(String itemId){
        // TODO 1 (Input): Nhận itemId.
        // TODO 2 (Processing): Kiểm tra điều kiện (Không được xóa nếu phiên đang chạy hoặc đã kết thúc).
        // TODO 3 (Side-effect): Gọi ItemDAO.deleteItem(...).

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        // Kiểm tra nếu session tồn tại VÀ trạng thái của nó không cho phép xóa
        if (session != null && (session.getStatus() == SessionStatus.RUNNING || session.getStatus() == SessionStatus.OPEN || session.getStatus() == SessionStatus.FINISHED)){
            throw new BusinessException("Không thể xóa vật phẩm khi phiên đấu giá đang mở, đang chạy hoặc đã kết thúc.");
        }

        ItemDAO.getInstance().deleteItem(itemId);
    }
}