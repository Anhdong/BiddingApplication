package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.SessionFilterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
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
        // TODO 1 (Input): Nhận sessionId và thông tin giá vừa đặt thành công (bidInfo).
        // TODO 2 (Dependencies): Lấy instance của AuctionSessionDAO và BidDAO.
        // TODO 3 (Processing): Gọi AuctionSessionDAO để cập nhật current_price và last_bidder.
        // TODO 4 (Processing): Gọi BidDAO để lưu lịch sử trả giá (bidInfo) vào database.

        User bidder = UserDAO.getInstance().findByUsername(bidInfo.getBidderName());
        if (bidder == null){
            throw new BusinessException("Không tìm thấy Bidder");
        }

        String bidderId = bidder.getId();
        BigDecimal newPrice = bidInfo.getBidAmount();

        boolean sessionUpdated = AuctionSessionDAO.getInstance().updatePriceAndWinner(sessionId, newPrice, bidderId);

        if (!sessionUpdated) {
            // Log lỗi nếu không thể cập nhật DB, nhưng không ném Exception
            // vì logic trên RAM đã hoàn tất thành công.
            System.err.println("Lỗi đồng bộ giá cho phiên: " + sessionId);
        }

        // 2. Lưu lịch sử lượt trả giá này vào bảng BidTransaction
        // Chuyển đổi từ DTO sang Entity hoặc truyền tham số trực tiếp tùy thiết kế DAO
        BidTransaction bidLog = new BidTransaction();
        bidLog.setSessionId(sessionId);
        bidLog.setBidAmount(newPrice);
        bidLog.setBidderId(bidderId);
        bidLog.setCreatedAt(bidInfo.getTime());

        boolean bidLogged = BidDAO.getInstance().insertBid(bidLog);

        if (!bidLogged) {
            System.err.println("Lỗi lưu lịch sử trả giá cho phiên: " + sessionId);
        }
    }
}