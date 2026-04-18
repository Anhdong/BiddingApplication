package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.SessionFilterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.BidType;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.BidTransaction;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.Utils.Mapper.AuctionViewMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lớp điều phối trung tâm lưu trữ dữ liệu động và xử lý sau khi có biến động giá.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class AuctionService {

    // Khởi tạo Singleton an toàn đa luồng
    private static volatile AuctionService instance = null;

    private AuctionService(){
        // TODO: Cần khởi tạo các instance của AuctionSessionDAO và BidDAO từ Tech Lead.
    }

    public static AuctionService getInstance(){
        if (instance == null){
            synchronized (AuctionService.class){
                if (instance == null){
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy danh sách các phiên đấu giá đang hoạt động dựa trên bộ lọc.
     */
    public List<AuctionCardDTO> getActiveSessions(SessionFilterRequestDTO filter){
        // TODO 1 (Input): Nhận bộ lọc filter (thể loại, thời gian).
        // TODO 2 (Dependencies): Lấy instance của ItemSearchService hoặc DAO tương ứng.
        // TODO 3 (Processing/Output): Gọi hàm lọc danh sách phiên và trả về List<AuctionCardDTO>.
        return null;
    }

    /**
     * Lấy thông tin chi tiết của một phiên đấu giá khi người dùng nhấn vào xem.
     */
    public SessionInfoResponseDTO getItemDetail(String sessionId){
        // TODO 1 (Input): Nhận ID của phiên (sessionId).
        // TODO 2 (Dependencies): Gọi SearchCacheManager hoặc AuctionSessionDAO để lấy chi tiết phiên và Item.
        // TODO 3 (Processing/Output): Map dữ liệu sang SessionInfoResponseDTO và trả về.

        AuctionSession session = SearchCacheManager.getInstance().getSession(sessionId);
        String itemId = session.getItemId();
        Item item = SearchCacheManager.getInstance().getItem(itemId);

        return AuctionViewMapper.toDetailDto(session, item);
    }

    /**
     * Hàm quan trọng: Cập nhật dữ liệu vào DB sau khi lõi đa luồng xác nhận giá hợp lệ.
     */
    public void updateSessionAfterValidBid(String sessionId, BidHistoryDTO bidInfo){
        // 1. Tìm User để lấy bidderId và KIỂM TRA NULL
        User bidder = UserDAO.getInstance().findByUsername(bidInfo.getBidderName());

        if (bidder == null) {
            System.err.println("Lỗi nghiêm trọng: Không tìm thấy User với tên: " + bidInfo.getBidderName());
            return; // Thoát hàm ngay lập tức để tránh lỗi sập hệ thống
        }

        String bidderId = bidder.getId();
        BigDecimal newPrice = bidInfo.getBidAmount();

        // 2. Gọi AuctionSessionDAO để cập nhật current_price và last_bidder
        boolean sessionUpdated = AuctionSessionDAO.getInstance().updatePriceAndWinner(sessionId, newPrice, bidderId);

        if (!sessionUpdated) {
            System.err.println("Lỗi đồng bộ giá cho phiên: " + sessionId);
        }

        // 3. Gọi BidDAO để lưu lịch sử trả giá vào database
        // ƯU TIÊN SỬ DỤNG CONSTRUCTOR CÓ THAM SỐ để tự động sinh UUID ở lớp cha Entity
        BidTransaction bidLog = new BidTransaction(
                bidderId,
                sessionId,
                newPrice,
                BidType.MANUAL // Thêm BidType.MANUAL (hoặc tùy logic) vì entity yêu cầu
        );

        // Đè lại thời gian chính xác từ RAM (DTO) truyền xuống thay vì thời gian lúc khởi tạo obj
        bidLog.setCreatedAt(bidInfo.getTime());

        boolean bidLogged = BidDAO.getInstance().insertBid(bidLog);

        if (!bidLogged) {
            System.err.println("Lỗi lưu lịch sử trả giá cho phiên: " + sessionId);
        }
    }
}