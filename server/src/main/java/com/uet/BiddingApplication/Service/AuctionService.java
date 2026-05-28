package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuctionService.class);

    private static volatile AuctionService instance = null;

    private AuctionService(){
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
        return null;
    }

    /**
     * Lấy thông tin chi tiết của một phiên đấu giá khi người dùng nhấn vào xem.
     */
    public SessionInfoResponseDTO getItemDetail(String sessionId){
        AuctionSession session = SearchCacheManager.getInstance().getSession(sessionId);
        Item item = null;

        if (session == null) {
            session = AuctionSessionDAO.getInstance().getSessionById(sessionId);

            if (session == null) {
                throw new BusinessException("Phiên đấu giá không tồn tại hoặc đã bị xóa.");
            }
        }

        String itemId = session.getItemId();
        item = SearchCacheManager.getInstance().getItem(itemId);

        if (item == null) {
            item = ItemDAO.getInstance().getItemById(itemId);
            if (item == null) {
                throw new BusinessException("Lỗi dữ liệu: Không tìm thấy sản phẩm của phiên đấu giá này.");
            }
        }

        return AuctionViewMapper.toDetailDto(session, item);
    }

    /**
     * Hàm cập nhật dữ liệu vào DB sau khi lõi đa luồng xác nhận giá hợp lệ.
     */
    public void updateSessionAfterValidBid(String sessionId, String bidderId, BidHistoryDTO bidInfo, BidType bidType) {
        if (bidderId == null || bidderId.isEmpty()) {
            throw new BusinessException("Lỗi hệ thống: Không thể xác định ID người trả giá.");
        }

        BigDecimal newPrice = bidInfo.getBidAmount();

        boolean sessionUpdated = AuctionSessionDAO.getInstance().updatePriceAndWinner(sessionId, newPrice, bidderId);
        if (!sessionUpdated) {
            throw new BusinessException("Lỗi đồng bộ: Không thể cập nhật giá mới cho phiên " + sessionId);
        }

        BidTransaction bidLog = new BidTransaction(
                bidderId,
                sessionId,
                newPrice,
                bidType
        );
        bidLog.setCreatedAt(bidInfo.getTime());

        boolean bidLogged = BidDAO.getInstance().insertBid(bidLog);
        if (!bidLogged) {
            throw new BusinessException("Lỗi hệ thống: Cập nhật giá thành công nhưng không thể lưu lịch sử.");
        }
    }
}