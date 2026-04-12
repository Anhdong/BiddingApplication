package com.uet.BiddingApplication.Service;

import java.util.List;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.Model.AuctionSession;

public class BidderService {

    // Khởi tạo Singleton
    private static volatile BidderService instance = null;

    // Constructor private để ngăn khởi tạo từ bên ngoài
    private BidderService(){
        // Tài liệu đặc tả không yêu cầu thuộc tính cho class này.
        // Bạn sẽ gọi trực tiếp các Singleton DAO bên trong các phương thức.
    }

    // Lấy instance duy nhất của BidderService
    public static BidderService getInstance() {
        // Kiểm tra lần 1: Bỏ qua khóa nếu đối tượng đã được tạo
        if (instance == null) {
            // Chỉ khóa class tại thời điểm khởi tạo lần đầu tiên
            synchronized (BidderService.class) {
                // Kiểm tra lần 2: Tránh trường hợp 2 luồng cùng lọt qua lần kiểm tra 1
                if (instance == null) {
                    instance = new BidderService();
                }
            }
        }
        return instance;
    }

    /**
     * Trả về danh sách các phiên mà Bidder này đã đăng ký tham gia trước.
     */
    public List<AuctionCardDTO> getRegisteredSessions(String bidderId){
        // TODO 1: Lấy instance của SessionRegistrationDAO (Do Tech Lead cung cấp)
        // TODO 2: Gọi phương thức lấy danh sách phiên đã đăng ký bằng ID của bidder
        // TODO 3: Trả về list danh sách đó (thay vì return null)

        // Cú pháp gợi ý (chờ Tech Lead hoàn thiện):
        // return SessionRegistrationDAO.getInstance().getRegisteredSessions(bidderId);

        return null;
    }

    /**
     * Trả về lịch sử tất cả các lần vung tiền của Bidder này.
     */
    public List<BidHistoryDTO> getBidderHistory(String bidderId){
        // TODO 1: Lấy instance của BidDAO (Do Tech Lead cung cấp)
        // TODO 2: Gọi phương thức lấy toàn bộ lịch sử vung tiền của user này
        // TODO 3: Nếu DAO trả về đúng kiểu List<BidHistoryDTO> thì return thẳng,
        //         nếu khác kiểu (ví dụ List<BidderHistoryResponseDTO>) thì cần viết vòng lặp để map/chuyển đổi dữ liệu.

        // Cú pháp gợi ý (chờ Tech Lead hoàn thiện):
        // return BidDAO.getInstance().getBidderHistory(bidderId);

        return null;
    }
}