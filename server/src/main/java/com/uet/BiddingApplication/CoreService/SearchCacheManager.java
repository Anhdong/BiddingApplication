package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.AuctionViewMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SearchCacheManager implements ISearchCacheManager{

    private static final SearchCacheManager INSTANCE = new SearchCacheManager();

    private final ConcurrentHashMap<String, AuctionSession> activeSessionsCache;
    private final ConcurrentHashMap<String, Item> itemCache;

    private SearchCacheManager() {
        this.activeSessionsCache = new ConcurrentHashMap<>(1024);
        this.itemCache = new ConcurrentHashMap<>(1024);
    }

    public static SearchCacheManager getInstance() {
        return INSTANCE;
    }

    // =========================================================================
    // 1. NHÓM PHƯƠNG THỨC KHỞI TẠO TỪ DATABASE (DAO)
    // =========================================================================

    /**
     * Nạp toàn bộ các phiên đang OPEN hoặc RUNNING từ DB lên RAM khi bật Server
     */
    @Override
    public void loadInitialData() {
        // Giả định AuctionSessionDAO có hàm lấy các phiên đang hoạt động
        List<AuctionSession> activeSessions = AuctionSessionDAO.getInstance().getAllSessions(true);
        if (activeSessions != null) {
            for (AuctionSession session : activeSessions) {
                activeSessionsCache.put(session.getId(), session);
            }
        }
    }

    /**
     * Dựa vào danh sách phiên đang chạy ở trên, kéo các Item tương ứng từ DB lên RAM
     */
    @Override
    public void loadInitialItems() {
        if (activeSessionsCache.isEmpty()) return;

        // Trích xuất tập hợp các itemId cần lấy
        List<String> itemIdsToFetch = new ArrayList<>();
        for(AuctionSession a:activeSessionsCache.values()){
            itemIdsToFetch.add(a.getItemId());
        }
        // Lấy danh sách Item
        List<Item> items = ItemDAO.getInstance().getItemsByIds(itemIdsToFetch);
        if (items != null) {
            for (Item item : items) {
                itemCache.put(item.getId(), item);
            }
        }
    }

    // =========================================================================
    // 2. NHÓM PHƯƠNG THỨC TRẢ VỀ DTO (GỌI MAPPER)
    // =========================================================================

    /**
     * Cung cấp dữ liệu thô dạng Map/List cho AuctionViewMapper để đóng gói thành List Card.
     * Thường được gọi bởi ItemSearchService khi User cần hiển thị danh sách màn hình chính.
     */
    @Override
    public List<AuctionCardDTO> getAllActiveSessionsAsCardDto() {
        // Rút toàn bộ session đang có trên RAM
        List<AuctionSession> sessions = new ArrayList<>(activeSessionsCache.values());

        // Chuyển việc lắp ráp dữ liệu cho Mapper xử lý tĩnh
        return AuctionViewMapper.toCardDTOList(sessions, itemCache);
    }

    /**
     * Lấy 1 phiên cụ thể từ RAM, móc nối với Item tương ứng và nhờ Mapper đóng gói Detail DTO.
     * Được gọi khi Bidder bấm vào 1 thẻ sản phẩm.
     */
    @Override
    public SessionInfoResponseDTO getSessionDetailDto(String sessionId) {
        return AuctionSessionDAO.getInstance().getSessionInfo(sessionId);
    }

    // =========================================================================
    // 3. NHÓM PHƯƠNG THỨC CRUD NỘI BỘ CACHE
    // =========================================================================
    @Override
    public void addSessionAndItem(AuctionSession session, Item item) {
        if (session != null && session.getId() != null) {
            activeSessionsCache.put(session.getId(), session);
        }
        if (item != null && item.getId() != null) {
            itemCache.put(item.getId(), item);
        }
    }
    @Override
    public void removeSession(String sessionId) {
        activeSessionsCache.remove(sessionId);
    }
    @Override
    public void removeItem(String itemId) {itemCache.remove(itemId);}
    @Override
    public void updatePriceInCache(String sessionId, BigDecimal newPrice, String highestBidderId) {
        AuctionSession session = activeSessionsCache.get(sessionId);
        if (session != null) {
            session.setCurrentPrice(newPrice);
            session.setWinnerId(highestBidderId);
        }
    }

    @Override
    public AuctionSession getSession(String sessionId) {
        return activeSessionsCache.get(sessionId);
    }

    @Override
    public List<AuctionSession> getActiveSessions() {
        return new ArrayList<>(activeSessionsCache.values());
    }
    @Override
    public Item getItem(String itemId) {
        return itemCache.get(itemId);
    }
}
