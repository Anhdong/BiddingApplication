package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.BidType;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BidDAOTest {

    private BidDAO bidDAO;
    private UserDAO userDAO;
    // Khai báo sẵn các ID để test
    private final String testBidderId = UUID.randomUUID().toString();
    private final String testSellerId = UUID.randomUUID().toString();
    private final String testSessionId = UUID.randomUUID().toString();
    private final String testItemId = UUID.randomUUID().toString();

    // Lưu lại ID của 2 lượt trả giá để test
    private final String bidId1 = UUID.randomUUID().toString();
    private final String bidId2 = UUID.randomUUID().toString();

    @BeforeAll
    void init() {
        bidDAO = BidDAO.getInstance(); // Giả định chuẩn Singleton
        userDAO= UserDAO.getInstance();
    }

    @BeforeEach
    void setUp() throws SQLException {
        cleanUpData(); // Xóa rác trước khi chạy

        // --- Chuẩn bị nhanh dữ liệu cha (Bắt buộc để không lỗi Foreign Key) ---
        User bidder = new Bidder(); bidder.setId(testBidderId); bidder.setUsername("bidder_test");
        bidder.setEmail("bidder_test@test");bidder.setPasswordHash("bidder_test");
        bidder.setRole(RoleType.BIDDER);
        userDAO.insertUser(bidder);

        User seller = new Seller(); seller.setId(testSellerId); seller.setUsername("seller_test");
        seller.setEmail("seller_test@test");seller.setPasswordHash("seller_test");
        seller.setRole(RoleType.SELLER);
        userDAO.insertUser(seller);


        Item item = new Art(); item.setId(testItemId); item.setSellerId(testSellerId);
        item.setCategory(Category.ART);item.setName("testItem");
        ItemDAO.getInstance().insertItem(item);

        AuctionSession session = new AuctionSession(); session.setId(testSessionId); session.setItemId(testItemId);
        session.setStartPrice(new BigDecimal("1000")); session.setStatus(SessionStatus.OPEN);
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(session.getStartTime());
        session.setSellerId(seller.getId());
        AuctionSessionDAO.getInstance().insertSession(session);
    }

    @AfterEach
    void tearDown() throws SQLException {
        cleanUpData(); // Trả lại DB sạch
    }

    private void cleanUpData() {
//        userDAO.deleteUser(testSellerId);
//        userDAO.deleteUser(testBidderId);
//        ItemDAO.getInstance().deleteItem(testItemId);
    }

    // ================== 1 PHƯƠNG THỨC TEST DUY NHẤT NHƯ YÊU CẦU ==================

    @Test
    @DisplayName("Kiểm tra luồng Bid: Insert -> Lấy Recent Bids -> Lấy Bidder History")
    void testBidFlow_RecentAndHistory() {
        // Bước 1: Giả lập user trả giá 2 lần liên tiếp (cách nhau 5 phút)
        BidTransaction bid1 = new BidTransaction();
        bid1.setId(bidId1); bid1.setBidderId(testBidderId); bid1.setSessionId(testSessionId);
        bid1.setBidAmount(new BigDecimal("1500")); bid1.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        bid1.setBidType(BidType.MANUAL);

        BidTransaction bid2 = new BidTransaction();
        bid2.setId(bidId2); bid2.setBidderId(testBidderId); bid2.setSessionId(testSessionId);
        bid2.setBidAmount(new BigDecimal("2000")); bid2.setCreatedAt(LocalDateTime.now()); // Mới hơn
        bid2.setBidType(BidType.MANUAL);

        assertTrue(bidDAO.insertBid(bid1) && bidDAO.insertBid(bid2), "Phải insert thành công 2 bid vào DB");

        // Bước 2: Test getRecentBids (Đảm bảo bid mới nhất lên đầu)
        List<BidHistoryDTO> recentBids = bidDAO.getRecentBids(testSessionId);
        assertFalse(recentBids.isEmpty(), "Danh sách bid không được rỗng");
        assertEquals(0, new BigDecimal("2000").compareTo(recentBids.get(0).getBidAmount()),
                "Bid mới nhất (2000) phải được ORDER BY DESC và nằm ở vị trí đầu tiên");
        // Bước 3: Test getBidderHistory (Đảm bảo thống kê đúng lịch sử của user)
        List<BidderHistoryResponseDTO> bidderHistory = bidDAO.getBidderHistory(testBidderId);
        assertFalse(bidderHistory.isEmpty(), "Lịch sử user không được rỗng");

        BidderHistoryResponseDTO historyDTO = bidderHistory.get(0);
        assertEquals(testSessionId, historyDTO.getSessionId(), "Phải map đúng phiên tham gia");
        assertEquals(0, new BigDecimal("2000").compareTo(recentBids.get(0).getBidAmount()),
                "Mức giá cao nhất trong test phải là 2000");
    }
}