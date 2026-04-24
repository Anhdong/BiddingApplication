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
        // 1. Cố gắng lấy từ Cache (RAM) trước để đảm bảo tốc độ siêu tốc
        AuctionSession session = SearchCacheManager.getInstance().getSession(sessionId);
        Item item = null;

        // 2. Cơ chế Fallback: Nếu không có trong Cache (ví dụ phiên đã kết thúc), tìm trong Database
        if (session == null) {
            session = AuctionSessionDAO.getInstance().getSessionById(sessionId);

            // Nếu DB cũng không có, ném lỗi nghiệp vụ theo chuẩn kiến trúc
            if (session == null) {
                throw new BusinessException("Phiên đấu giá không tồn tại hoặc đã bị xóa.");
            }
        }

        // 3. Lấy thông tin Item tương ứng (cũng áp dụng cơ chế Fallback tương tự)
        String itemId = session.getItemId();
        item = SearchCacheManager.getInstance().getItem(itemId);

        if (item == null) {
            item = ItemDAO.getInstance().getItemById(itemId);
            if (item == null) {
                throw new BusinessException("Lỗi dữ liệu: Không tìm thấy sản phẩm của phiên đấu giá này.");
            }
        }

        // 4. Map dữ liệu sang DTO và trả về
        return AuctionViewMapper.toDetailDto(session, item);
    }

    /**
     * Hàm cập nhật dữ liệu vào DB sau khi lõi đa luồng xác nhận giá hợp lệ.
     */
    public void updateSessionAfterValidBid(String sessionId, String bidderId, BidHistoryDTO bidInfo, BidType bidType) {
        // 1. Kiểm tra đầu vào thay vì truy vấn DB
        if (bidderId == null || bidderId.isEmpty()) {
            throw new BusinessException("Lỗi hệ thống: Không thể xác định ID người trả giá.");
        }

        BigDecimal newPrice = bidInfo.getBidAmount();

        // 2. Cập nhật phiên đấu giá
        boolean sessionUpdated = AuctionSessionDAO.getInstance().updatePriceAndWinner(sessionId, newPrice, bidderId);
        if (!sessionUpdated) {
            throw new BusinessException("Lỗi đồng bộ: Không thể cập nhật giá mới cho phiên " + sessionId);
        }

        // 3. Lưu lịch sử trả giá
        BidTransaction bidLog = new BidTransaction(
                bidderId,
                sessionId,
                newPrice,
                bidType // Động hóa BidType thay vì hardcode
        );
        bidLog.setCreatedAt(bidInfo.getTime()); // Đồng bộ thời gian thực tế từ RAM

        boolean bidLogged = BidDAO.getInstance().insertBid(bidLog);
        if (!bidLogged) {
            // Lưu ý: Nếu bước này lỗi, dữ liệu giữa 2 bảng đang bị lệch.
            throw new BusinessException("Lỗi hệ thống: Cập nhật giá thành công nhưng không thể lưu lịch sử.");
        }
    }
}