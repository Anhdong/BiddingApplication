package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.AuctionViewMapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemSearchService {

    private static final ItemSearchService INSTANCE = new ItemSearchService();

    private ItemSearchService() {}

    public static ItemSearchService getInstance() {
        return INSTANCE;
    }

    /**
     * Hàm tìm kiếm toàn năng sử dụng Java 8 Streams
     */
    public List<AuctionCardDTO> searchActiveAuctions(String keyword, Category category, String timeSortOption) {

        // 1. Lấy toàn bộ danh sách phiên thô từ Cache (chưa qua Mapper)
        SearchCacheManager searchCacheManager = SearchCacheManager.getInstance();
        List<AuctionSession> allSessions = searchCacheManager.getActiveSessions();

        Stream<AuctionSession> stream = allSessions.stream();

        // 2. Lọc theo Category (Nếu có)
        if (category != null) {
            stream = stream.filter(session -> {
                Item item = SearchCacheManager.getInstance().getItem(session.getItemId());
                return item != null && item.getCategory() == category;
            });
        }

        // 3. Lọc theo Keyword (Nếu có, tìm trong tên sản phẩm)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase().trim();
            stream = stream.filter(session -> {
                Item item = SearchCacheManager.getInstance().getItem(session.getItemId());
                return item != null && item.getName().toLowerCase().contains(lowerKeyword);
            });
        }

        // 4. Sắp xếp theo Thời gian (Time Sort)
        if (timeSortOption != null) {
            switch (timeSortOption.toUpperCase()) {
                case "ENDING_SOON":
                    // Sắp xếp thời gian kết thúc tăng dần (Càng gần hiện tại càng lên đầu)
                    stream = stream.sorted(Comparator.comparing(AuctionSession::getEndTime));
                    break;
                case "NEWEST":
                    // Mới nhất: Sắp xếp thời gian bắt đầu giảm dần
                    stream = stream.sorted(Comparator.comparing(AuctionSession::getStartTime).reversed());
                    break;
                case "OLDEST":
                    // Cũ nhất: Sắp xếp thời gian bắt đầu tăng dần
                    stream = stream.sorted(Comparator.comparing(AuctionSession::getStartTime));
                    break;
            }
        }

        // 5. Sau khi lọc và sort xong, mới map các phần tử còn lại sang DTO
        return stream.map(session -> {
            Item item = SearchCacheManager.getInstance().getItem(session.getItemId());
            return AuctionViewMapper.toCardDTO(session, item);
        }).collect(Collectors.toList());
    }
}