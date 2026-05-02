package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.BidType;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.BidTransaction;
import com.uet.BiddingApplication.Model.Electronics;
import com.uet.BiddingApplication.Model.Item;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    private AuctionService auctionService;

    // 1. Khai báo Mock Object để cô lập hệ thống
    @Mock private SearchCacheManager mockCacheManager;
    @Mock private AuctionSessionDAO mockSessionDAO;
    @Mock private ItemDAO mockItemDAO;
    @Mock private BidDAO mockBidDAO;

    @BeforeEach
    void setUp() throws Exception {
        auctionService = AuctionService.getInstance();

        // 2. Tiêm Mock vào các Singleton bằng Reflection
        injectSingleton(SearchCacheManager.class, mockCacheManager);
        injectSingleton(AuctionSessionDAO.class, mockSessionDAO);
        injectSingleton(ItemDAO.class, mockItemDAO);
        injectSingleton(BidDAO.class, mockBidDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 3. Trả lại trạng thái sạch sau mỗi bài test
        injectSingleton(SearchCacheManager.class, null);
        injectSingleton(AuctionSessionDAO.class, null);
        injectSingleton(ItemDAO.class, null);
        injectSingleton(BidDAO.class, null);
    }

    // --- Helper Method ---
    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    // ==========================================
    // TEST CASES: getItemDetail (Luồng Fallback Cache -> DB)
    // ==========================================

    @Test
    @DisplayName("getItemDetail: Thành công khi lấy dữ liệu hoàn toàn từ Cache RAM")
    void testGetItemDetail_Success_FromCache() {
        String sessionId = "session-1";
        String itemId = "item-1";

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setItemId(itemId);

        // Đa hình: Khởi tạo Electronics thay vì Item trừu tượng
        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setCategory(Category.ELECTRONICS);
        mockItem.setName("Điện thoại Test");

        // Giả lập RAM có sẵn dữ liệu
        when(mockCacheManager.getSession(sessionId)).thenReturn(mockSession);
        when(mockCacheManager.getItem(itemId)).thenReturn(mockItem);

        // Act
        SessionInfoResponseDTO result = auctionService.getItemDetail(sessionId);

        // Assert
        assertNotNull(result, "DTO trả về không được rỗng");

        // Đảm bảo tuyệt đối không gọi xuống DB
        verifyNoInteractions(mockSessionDAO);
        verifyNoInteractions(mockItemDAO);
    }

    @Test
    @DisplayName("getItemDetail: Thành công khi Cache trống, Fallback chọc xuống DB")
    void testGetItemDetail_Success_FromDB_Fallback() {
        String sessionId = "session-1";
        String itemId = "item-1";

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setItemId(itemId);

        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setCategory(Category.ELECTRONICS);

        // Giả lập RAM bị trống
        when(mockCacheManager.getSession(sessionId)).thenReturn(null);
        when(mockCacheManager.getItem(itemId)).thenReturn(null);

        // Giả lập DB trả về dữ liệu
        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(mockSession);
        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);

        // Act
        SessionInfoResponseDTO result = auctionService.getItemDetail(sessionId);

        // Assert
        assertNotNull(result);
        verify(mockSessionDAO, times(1)).getSessionById(sessionId);
        verify(mockItemDAO, times(1)).getItemById(itemId);
    }

    @Test
    @DisplayName("getItemDetail: Thất bại bắn lỗi do phiên không tồn tại ở cả RAM và DB")
    void testGetItemDetail_Fail_SessionNotFound() {
        String sessionId = "session-invalid";

        when(mockCacheManager.getSession(sessionId)).thenReturn(null);
        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> auctionService.getItemDetail(sessionId));

        assertEquals("Phiên đấu giá không tồn tại hoặc đã bị xóa.", exception.getMessage());
    }

    // ==========================================
    // TEST CASES: updateSessionAfterValidBid (Đồng bộ dữ liệu)
    // ==========================================

    @Test
    @DisplayName("updateSessionAfterValidBid: Thành công cập nhật giá và lưu lịch sử")
    void testUpdateSessionAfterValidBid_Success() {
        String sessionId = "session-1";
        String bidderId = "bidder-1";
        BidHistoryDTO bidInfo = new BidHistoryDTO("Test User", new BigDecimal("5000"), LocalDateTime.now(), sessionId);

        when(mockSessionDAO.updatePriceAndWinner(sessionId, bidInfo.getBidAmount(), bidderId)).thenReturn(true);
        when(mockBidDAO.insertBid(any(BidTransaction.class))).thenReturn(true);

        // Act - Không văng lỗi là thành công
        assertDoesNotThrow(() ->
                auctionService.updateSessionAfterValidBid(sessionId, bidderId, bidInfo, BidType.MANUAL)
        );

        // Đảm bảo cả 2 thao tác DB đều được gọi
        verify(mockSessionDAO, times(1)).updatePriceAndWinner(sessionId, bidInfo.getBidAmount(), bidderId);
        verify(mockBidDAO, times(1)).insertBid(any(BidTransaction.class));
    }

    @Test
    @DisplayName("updateSessionAfterValidBid: Thất bại do ID người trả giá rỗng")
    void testUpdateSessionAfterValidBid_Fail_EmptyBidderId() {
        BidHistoryDTO bidInfo = new BidHistoryDTO();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> auctionService.updateSessionAfterValidBid("session-1", "", bidInfo, BidType.MANUAL));

        assertEquals("Lỗi hệ thống: Không thể xác định ID người trả giá.", exception.getMessage());

        // Cực kỳ quan trọng: Lỗi sớm (Fail-fast) thì không được chạm vào DB
        verifyNoInteractions(mockSessionDAO);
        verifyNoInteractions(mockBidDAO);
    }

    @Test
    @DisplayName("updateSessionAfterValidBid: Thất bại do không thể cập nhật Session")
    void testUpdateSessionAfterValidBid_Fail_SessionUpdateError() {
        String sessionId = "session-1";
        String bidderId = "bidder-1";
        BidHistoryDTO bidInfo = new BidHistoryDTO("Test User", new BigDecimal("5000"), LocalDateTime.now(), sessionId);

        // Giả lập DB báo lỗi khi cập nhật giá
        when(mockSessionDAO.updatePriceAndWinner(sessionId, bidInfo.getBidAmount(), bidderId)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> auctionService.updateSessionAfterValidBid(sessionId, bidderId, bidInfo, BidType.MANUAL));

        assertTrue(exception.getMessage().contains("Không thể cập nhật giá mới"));

        // Đảm bảo nếu cập nhật Session lỗi, hàm phải dừng lại và KHÔNG ĐƯỢC insert lịch sử giá
        verify(mockBidDAO, never()).insertBid(any());
    }

    @Test
    @DisplayName("updateSessionAfterValidBid: Thất bại do không thể lưu lịch sử Bid")
    void testUpdateSessionAfterValidBid_Fail_BidInsertError() {
        String sessionId = "session-1";
        String bidderId = "bidder-1";
        BidHistoryDTO bidInfo = new BidHistoryDTO("Test User", new BigDecimal("5000"), LocalDateTime.now(), sessionId);

        when(mockSessionDAO.updatePriceAndWinner(sessionId, bidInfo.getBidAmount(), bidderId)).thenReturn(true);
        // Giả lập DB báo lỗi khóa ngoại hoặc rớt mạng khi insert lịch sử
        when(mockBidDAO.insertBid(any(BidTransaction.class))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> auctionService.updateSessionAfterValidBid(sessionId, bidderId, bidInfo, BidType.MANUAL));

        assertTrue(exception.getMessage().contains("không thể lưu lịch sử"));
    }
}