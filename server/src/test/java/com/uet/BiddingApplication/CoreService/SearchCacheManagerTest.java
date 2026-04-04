package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.Model.Art;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Model.Others;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchCacheManagerTest {

    private SearchCacheManager cacheManager;

    @Mock
    private AuctionSessionDAO mockSessionDAO;

    @Mock
    private ItemDAO mockItemDAO;

    @BeforeEach
    void setUp() throws Exception {
        cacheManager = SearchCacheManager.getInstance();

        // 1. Dọn dẹp RAM (Cache) trước mỗi test bằng Reflection
        // Lý do: CacheManager là Singleton, nếu không dọn dẹp, data từ test trước sẽ lọt sang test sau
        clearCacheMap("activeSessionsCache");
        clearCacheMap("itemCache");

        // 2. Bơm (Inject) Mock Object vào các lớp DAO Singleton mà KHÔNG CẦN dùng mockStatic
        injectMockIntoSingleton(AuctionSessionDAO.class, mockSessionDAO);
        injectMockIntoSingleton(ItemDAO.class, mockItemDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Trả lại môi trường sạch sẽ (gỡ Mock ra khỏi Singleton)
        injectMockIntoSingleton(AuctionSessionDAO.class, null);
        injectMockIntoSingleton(ItemDAO.class, null);
    }

    // ================== CÁC HÀM TIỆN ÍCH CHO TEST ==================

    private void clearCacheMap(String fieldName) throws Exception {
        Field field = SearchCacheManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ConcurrentHashMap<?, ?> map = (ConcurrentHashMap<?, ?>) field.get(cacheManager);
        map.clear();
    }

    private void injectMockIntoSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        // Giả định biến lưu Singleton trong DAO của bạn tên là "instance"
        Field instanceField = clazz.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mockInstance);
    }

    // ================== CÁC TEST CASE CHÍNH ==================

    @Test
    @DisplayName("Test CRUD nội bộ RAM: Thêm, Lấy, và Xóa dữ liệu Cache")
    void testCacheOperations() {
        // Arrange
        AuctionSession session = new AuctionSession();
        session.setId("session-1");
        Item item = new Art();
        item.setId("item-1");

        // Act: Thêm vào
        cacheManager.addSessionAndItem(session, item);

        // Assert: Lấy ra kiểm tra
        assertEquals(session, cacheManager.getSession("session-1"), "Phiên phải tồn tại trong cache");
        assertEquals(item, cacheManager.getItem("item-1"), "Vật phẩm phải tồn tại trong cache");

        // Act: Xóa
        cacheManager.removeSession("session-1");

        // Assert: Kiểm tra sau khi xóa
        assertNull(cacheManager.getSession("session-1"), "Phiên phải bị xóa khỏi cache");
    }

    @Test
    @DisplayName("Test updatePriceInCache: Cập nhật giá và người thắng nhanh trên RAM")
    void testUpdatePriceInCache() {
        // Arrange
        AuctionSession session = new AuctionSession();
        session.setId("session-1");
        session.setCurrentPrice(new BigDecimal("100"));
        cacheManager.addSessionAndItem(session, null);

        // Act
        cacheManager.updatePriceInCache("session-1", new BigDecimal("500"), "winner-99");

        // Assert
        AuctionSession updatedSession = cacheManager.getSession("session-1");
        assertEquals(new BigDecimal("500"), updatedSession.getCurrentPrice(), "Giá mới phải được cập nhật");
        assertEquals("winner-99", updatedSession.getWinnerId(), "Người thắng mới phải được cập nhật");
    }

    @Test
    @DisplayName("Test loadInitialData: Kéo dữ liệu từ DB (thông qua Mock) lên RAM thành công")
    void testLoadInitialData() {
        // Arrange: Cấu hình Mock trả về 2 phiên giả lập
        AuctionSession session1 = new AuctionSession(); session1.setId("s1");
        AuctionSession session2 = new AuctionSession(); session2.setId("s2");
        when(mockSessionDAO.getAllSessions(true)).thenReturn(Arrays.asList(session1, session2));

        // Act
        cacheManager.loadInitialData();

        // Assert
        assertEquals(2, cacheManager.getActiveSessions().size(), "Cache phải chứa đúng 2 phiên từ DB");
        assertNotNull(cacheManager.getSession("s1"));
        verify(mockSessionDAO, times(1)).getAllSessions(true); // Đảm bảo hàm DAO thực sự được gọi
    }

    @Test
    @DisplayName("Test loadInitialItems: Kéo Items tương ứng với các Session có trong RAM")
    void testLoadInitialItems() {
        // Arrange: Đưa 1 phiên vào cache trước
        AuctionSession session = new AuctionSession();
        session.setId("s1");
        session.setItemId("item-1");
        cacheManager.addSessionAndItem(session, null);

        Item mockItem = new Others();
        mockItem.setId("item-1");

        // Cấu hình Mock để khi class hỏi xin item-1 thì trả về mockItem
        when(mockItemDAO.getItemsByIds(new ArrayList<>(Arrays.asList("item-1")))).thenReturn(new ArrayList<>(Arrays.asList(mockItem)));

        // Act
        cacheManager.loadInitialItems();

        // Assert
        assertNotNull(cacheManager.getItem("item-1"), "Item phải được load vào cache thành công");
    }
}
