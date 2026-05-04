package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.DTO.Request.RelistRequestDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Electronics;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.StorageService;
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
public class ItemManagementServiceTest {

    private ItemManagementService itemManagementService;

    // 1. Khai báo tới 5 Mock Object để cô lập hoàn toàn môi trường
    @Mock private ItemDAO mockItemDAO;
    @Mock private AuctionSessionDAO mockSessionDAO;
    @Mock private StorageService mockStorageService;
    @Mock private SearchCacheManager mockCacheManager;
    @Mock private SessionStartScheduler mockScheduler;

    @BeforeEach
    void setUp() throws Exception {
        itemManagementService = ItemManagementService.getInstance();

        // 2. Tiêm toàn bộ các Mock vào Singleton thông qua Reflection
        injectSingleton(ItemDAO.class, mockItemDAO);
        injectSingleton(AuctionSessionDAO.class, mockSessionDAO);
        injectSingleton(StorageService.class, mockStorageService);
        injectSingleton(SearchCacheManager.class, mockCacheManager);
        injectSingleton(SessionStartScheduler.class, mockScheduler);

        // Vì class này dùng biến DAO cục bộ khởi tạo trong constructor, ta phải tiêm thêm vào thuộc tính của nó
        injectPrivateField(itemManagementService, "itemDAO", mockItemDAO);
        injectPrivateField(itemManagementService, "sessionDAO", mockSessionDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 3. Dọn dẹp môi trường sạch sẽ sau mỗi test case
        injectSingleton(ItemDAO.class, null);
        injectSingleton(AuctionSessionDAO.class, null);
        injectSingleton(StorageService.class, null);
        injectSingleton(SearchCacheManager.class, null);
        injectSingleton(SessionStartScheduler.class, null);
    }

    // --- Helper Methods ---
    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ==========================================
    // TEST CASES: createItemAndOpenSession
    // ==========================================

    @Test
    @DisplayName("createItemAndOpenSession: Thành công tạo Item, mở Session và lên lịch")
    void testCreateItemAndOpenSession_Success() throws Exception {
        // Arrange
        String sellerId = "seller-1";
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(2);

        ItemCreateDTO request = new ItemCreateDTO("Laptop", "Gaming", "ELECTRONICS",
                new byte[]{1, 2, 3}, "png", new BigDecimal("1000"), startTime, endTime, "12 months");

        when(mockStorageService.uploadImage(any(byte[].class), anyString())).thenReturn("http://image.url");
        when(mockItemDAO.insertItem(any(Item.class))).thenReturn(true);
        when(mockSessionDAO.insertSession(any(AuctionSession.class))).thenReturn(true);

        // Act
        boolean result = itemManagementService.createItemAndOpenSession(request, sellerId);

        // Assert
        assertTrue(result);
        verify(mockStorageService, times(1)).uploadImage(any(byte[].class), anyString());
        verify(mockItemDAO, times(1)).insertItem(any(Item.class));
        verify(mockSessionDAO, times(1)).insertSession(any(AuctionSession.class));
        verify(mockCacheManager, times(1)).addSessionAndItem(any(AuctionSession.class), any(Item.class));
        verify(mockScheduler, times(1)).scheduleStart(any(), eq(startTime)); // Đảm bảo Scheduler được gọi
    }

    @Test
    @DisplayName("createItemAndOpenSession: Thất bại do lỗi Storage upload ảnh")
    void testCreateItemAndOpenSession_Fail_StorageError() throws Exception {
        String sellerId = "seller-1";
        ItemCreateDTO request = new ItemCreateDTO();
        request.setImageBytes(new byte[]{1});

        // Giả lập hệ thống lưu trữ quăng lỗi
        when(mockStorageService.uploadImage(any(), any())).thenThrow(new RuntimeException("Cloud Error"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> itemManagementService.createItemAndOpenSession(request, sellerId));

        assertEquals("Không thể tải lên hình ảnh sản phẩm. Vui lòng thử lại.", exception.getMessage());

        // Cực kỳ quan trọng: Lỗi sớm (Fail-fast) thì tuyệt đối không được gọi DB
        verifyNoInteractions(mockItemDAO);
        verifyNoInteractions(mockSessionDAO);
    }

    // ==========================================
    // TEST CASES: relistUnsoldItem
    // ==========================================

    @Test
    @DisplayName("relistUnsoldItem: Thành công cập nhật trực tiếp vì phiên đang OPEN")
    void testRelistUnsoldItem_Success_UpdateOpenSession() {
        // Arrange
        String sellerId = "seller-1";
        String itemId = "item-1";
        String sessionId = "session-1";
        LocalDateTime newStartTime = LocalDateTime.now().plusHours(1);

        RelistRequestDTO request = new RelistRequestDTO(itemId, sessionId, new BigDecimal("1500"),
                newStartTime, LocalDateTime.now().plusDays(1));

        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setSellerId(sellerId);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setStatus(SessionStatus.OPEN);

        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);
        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(mockSession);
        when(mockSessionDAO.updateSession(any(AuctionSession.class))).thenReturn(true); // Sẽ gọi Update

        // Act
        boolean result = itemManagementService.relistUnsoldItem(request, sellerId);

        // Assert
        assertTrue(result);
        verify(mockSessionDAO, times(1)).updateSession(mockSession);
        verify(mockSessionDAO, never()).insertSession(any());
        verify(mockCacheManager, times(1)).addSessionAndItem(mockSession, mockItem);
        verify(mockScheduler, times(1)).scheduleStart(sessionId, newStartTime);
    }

    @Test
    @DisplayName("relistUnsoldItem: Thành công tạo phiên mới vì phiên cũ đã FINISHED")
    void testRelistUnsoldItem_Success_CreateNewSession() {
        // Arrange
        String sellerId = "seller-1";
        String itemId = "item-1";
        String sessionId = "session-old";

        RelistRequestDTO request = new RelistRequestDTO(itemId, sessionId, new BigDecimal("1500"),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1));

        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setSellerId(sellerId);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setStatus(SessionStatus.FINISHED); // Trạng thái đã kết thúc

        when(mockItemDAO.getItemById(itemId)).thenReturn(mockItem);
        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(mockSession);
        when(mockSessionDAO.insertSession(any(AuctionSession.class))).thenReturn(true); // Sẽ gọi Insert

        // Act
        boolean result = itemManagementService.relistUnsoldItem(request, sellerId);

        // Assert
        assertTrue(result);
        verify(mockSessionDAO, never()).updateSession(any());
        verify(mockSessionDAO, times(1)).insertSession(any(AuctionSession.class)); // Bắt buộc tạo mới
    }

    @Test
    @DisplayName("relistUnsoldItem: Thất bại do người dùng không phải chủ sở hữu Item")
    void testRelistUnsoldItem_Fail_NotOwner() {
        RelistRequestDTO request = new RelistRequestDTO("item-1", "session-1", new BigDecimal("100"),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1));

        Item mockItem = new Electronics();
        mockItem.setSellerId("ANOTHER_SELLER"); // Không khớp

        when(mockItemDAO.getItemById("item-1")).thenReturn(mockItem);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> itemManagementService.relistUnsoldItem(request, "seller-1"));

        assertEquals("Bạn không có quyền thao tác với vật phẩm này.", exception.getMessage());
    }

    @Test
    @DisplayName("relistUnsoldItem: Thất bại do đặt thời gian trong quá khứ")
    void testRelistUnsoldItem_Fail_PastTime() {
        RelistRequestDTO request = new RelistRequestDTO("item-1", "session-1", new BigDecimal("100"),
                LocalDateTime.now().minusDays(1), // Cố tình đặt ở quá khứ
                LocalDateTime.now().plusDays(1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> itemManagementService.relistUnsoldItem(request, "seller-1"));

        assertEquals("Thời gian bắt đầu không thể diễn ra trong quá khứ.", exception.getMessage());
    }

    @Test
    @DisplayName("relistUnsoldItem: Thất bại do cố ý tác động vào phiên đang RUNNING")
    void testRelistUnsoldItem_Fail_SessionRunning() {
        RelistRequestDTO request = new RelistRequestDTO("item-1", "session-1", new BigDecimal("100"),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1));

        Item mockItem = new Electronics();
        mockItem.setId("item-1");
        mockItem.setSellerId("seller-1");

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.RUNNING); // Đang chạy

        when(mockItemDAO.getItemById("item-1")).thenReturn(mockItem);
        when(mockSessionDAO.getSessionById("session-1")).thenReturn(mockSession);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> itemManagementService.relistUnsoldItem(request, "seller-1"));

        assertEquals("Không thể chỉnh sửa hoặc đăng lại khi phiên đấu giá đang diễn ra.", exception.getMessage());
    }
}