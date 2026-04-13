package com.uet.BiddingApplication.Service;

import java.util.List;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.DTO.Request.ItemUpdateRequestDTO;

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
        return null;
    }

    /**
     * Cập nhật thông tin vật phẩm (Chỉ hợp lệ khi phiên chưa bắt đầu).
     */
    public void updateItem(ItemUpdateRequestDTO request, String itemId){
        // TODO 1 (Input): Nhận thông tin cần sửa từ request.
        // TODO 2 (Processing): Kiểm tra trạng thái phiên, nếu đang RUNNING thì chặn ném Exception.
        // TODO 3 (Side-effect): Gọi ItemDAO.updateItem(...) để ghi xuống cơ sở dữ liệu.
    }

    /**
     * Xóa vật phẩm khỏi hệ thống.
     */
    public void deleteItem(String itemId){
        // TODO 1 (Input): Nhận itemId.
        // TODO 2 (Processing): Kiểm tra điều kiện (Không được xóa nếu phiên đang chạy hoặc đã kết thúc).
        // TODO 3 (Side-effect): Gọi ItemDAO.deleteItem(...).
    }
}