package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.InMemoryBidServiceImpl;
import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.AdminActionRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    private AdminService adminService;

    // Khai báo các đối tượng Mock để cách ly AdminService khỏi Database và Network
    @Mock private UserDAO mockUserDAO;
    @Mock private AuctionSessionDAO mockSessionDAO;
    @Mock private AuctionServer mockAuctionServer;
    @Mock private SearchCacheManager mockCacheManager;
    @Mock private RealtimeBroadcastService mockBroadcastService;
    @Mock private InMemoryBidServiceImpl mockInMemoryBidService;
    @Mock private ItemDAO mockItemDAO;
    @Mock private AutoBidManager mockAutoBidManager;
    @Mock private SessionStartScheduler mockScheduler;

    @BeforeEach
    void setUp() throws Exception {
        adminService = AdminService.getInstance();

        // Sử dụng Reflection để tiêm các Mock vào các class Singleton
        injectSingleton(UserDAO.class, mockUserDAO);
        injectSingleton(AuctionSessionDAO.class, mockSessionDAO);
        injectSingleton(AuctionServer.class, mockAuctionServer);
        injectSingleton(SearchCacheManager.class, mockCacheManager);
        injectSingleton(RealtimeBroadcastService.class, mockBroadcastService);
        injectSingleton(ItemDAO.class, mockItemDAO);
        injectSingleton(InMemoryBidServiceImpl.class, mockInMemoryBidService);
        injectSingleton(AutoBidManager.class, mockAutoBidManager);
        injectSingleton(SessionStartScheduler.class, mockScheduler);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Trả lại trạng thái sạch cho các Singleton sau mỗi bài test
        injectSingleton(UserDAO.class, null);
        injectSingleton(AuctionSessionDAO.class, null);
        injectSingleton(AuctionServer.class, null);
        injectSingleton(SearchCacheManager.class, null);
        injectSingleton(RealtimeBroadcastService.class, null);
        injectSingleton(ItemDAO.class, null);
        injectSingleton(AutoBidManager.class, null);
        injectSingleton(InMemoryBidServiceImpl.class, null);
        injectSingleton(SessionStartScheduler.class, null);
    }

    // --- Helper Method ---
    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    // ==========================================
    // TEST CASES CHO TÍNH NĂNG GET ALL USERS
    // ==========================================

    @Test
    @DisplayName("getAllUsers: Thành công trả về danh sách UserProfileDTO")
    void testGetAllUsers_Success() {
        User user1 = new Bidder();
        user1.setId("user-1");
        user1.setUsername("bidder1");
        user1.setEmail("bidder1@test.com");
        user1.setRole(RoleType.BIDDER);

        User user2 = new Seller();
        user2.setId("user-2");
        user2.setUsername("seller1");
        user2.setEmail("seller1@test.com");
        user2.setRole(RoleType.SELLER);

        when(mockUserDAO.getAllUsers()).thenReturn(Arrays.asList(user1, user2));

        List<UserProfileDTO> result = adminService.getAllUsers();

        assertEquals(2, result.size(), "Phải trả về đúng số lượng UserProfileDTO");
        assertEquals("bidder1", result.get(0).getUsername());
        assertEquals("seller1", result.get(1).getUsername());
        verify(mockUserDAO, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("getAllUsers: Trả về danh sách rỗng khi không có user")
    void testGetAllUsers_Empty() {
        when(mockUserDAO.getAllUsers()).thenReturn(null);

        List<UserProfileDTO> result = adminService.getAllUsers();

        assertNotNull(result, "Không được trả về null");
        assertTrue(result.isEmpty(), "Danh sách phải rỗng khi DAO trả về null");
    }

    // ==========================================
    // TEST CASES CHO TÍNH NĂNG GET ALL SESSIONS
    // ==========================================

    @Test
    @DisplayName("getAllSessions: Thành công trả về danh sách AuctionCardDTO với item mapping")
    void testGetAllSessions_Success() {
        String itemId = "item-1";
        AuctionSession session = new AuctionSession();
        session.setId("session-1");
        session.setItemId(itemId);
        session.setStatus(SessionStatus.OPEN);

        Item mockItem = new Electronics();
        mockItem.setId(itemId);
        mockItem.setName("Laptop Test");

        when(mockSessionDAO.getAllSessions(true)).thenReturn(Collections.singletonList(session));
        when(mockItemDAO.getItemsByIds(anyList())).thenReturn(Collections.singletonList(mockItem));

        List<AuctionCardDTO> result = adminService.getAllSessions();

        assertFalse(result.isEmpty(), "Danh sách phải có ít nhất 1 phần tử");
        assertEquals(1, result.size());
        verify(mockSessionDAO, times(1)).getAllSessions(true);
        verify(mockItemDAO, times(1)).getItemsByIds(anyList());
    }

    @Test
    @DisplayName("getAllSessions: Trả về danh sách rỗng khi không có session")
    void testGetAllSessions_Empty() {
        when(mockSessionDAO.getAllSessions(true)).thenReturn(null);

        List<AuctionCardDTO> result = adminService.getAllSessions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==========================================
    // TEST CASES CHO TÍNH NĂNG BAN USER
    // ==========================================

    @Test
    @DisplayName("Ban User: Thành công khi Admin có OTP đúng và mục tiêu hợp lệ")
    void testBanUser_Success() {
        // 1. Arrange (Chuẩn bị dữ liệu)
        String adminId = "admin-1";
        String targetId = "bidder-1";
        String validOTP = "123456";

        Admin mockAdmin = new Admin();
        mockAdmin.setId(adminId);
        mockAdmin.setRole(RoleType.ADMIN);
        mockAdmin.setSecretKey(validOTP);

        User mockTarget = new Bidder();
        mockTarget.setId(targetId);
        mockTarget.setRole(RoleType.BIDDER);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId(targetId);
        request.setKey(validOTP);

        // Cấu hình hành vi cho Mock
        when(mockUserDAO.findById(adminId)).thenReturn(mockAdmin);
        when(mockUserDAO.findById(targetId)).thenReturn(mockTarget);
        when(mockUserDAO.updateStatus(targetId, false)).thenReturn(true);

        // 2. Act (Thực thi)
        boolean result = adminService.banUser(request, adminId);

        // 3. Assert (Kiểm chứng)
        assertTrue(result, "Hàm phải trả về true khi Ban thành công");
        verify(mockUserDAO, times(1)).updateStatus(targetId, false);
        verify(mockAuctionServer, times(1)).kickUser(targetId); // Đảm bảo Socket bị ngắt
        verify(mockAutoBidManager, times(1)).removeAutoBidsForBannedUser(targetId); // Đảm bảo Auto-bid bị dọn dẹp
    }

    @Test
    @DisplayName("Ban User: Thất bại và bắn lỗi khi nhập sai OTP")
    void testBanUser_Fail_WrongOTP() {
        Admin mockAdmin = new Admin();
        mockAdmin.setSecretKey("123456"); // OTP đúng

        when(mockUserDAO.findById("admin-1")).thenReturn(mockAdmin);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId("bidder-1");
        request.setKey("999999"); // Cố tình nhập sai

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminService.banUser(request, "admin-1"));

        assertEquals("Mã OTP xác thực không chính xác.", exception.getMessage());
        verify(mockAuctionServer, never()).kickUser(anyString()); // Đảm bảo không ai bị kick oan
    }

    // ==========================================
    // TEST CASES CHO TÍNH NĂNG CANCEL SESSION
    // ==========================================

    @Test
    @DisplayName("Cancel Session: Thành công hủy phiên đang OPEN")
    void testCancelSession_Success() {
        // 1. Arrange (Chuẩn bị dữ liệu)
        String adminId = "admin-1";
        String sessionId = "session-1";
        String validOTP = "654321";

        Admin mockAdmin = new Admin();
        mockAdmin.setSecretKey(validOTP);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setStatus(SessionStatus.OPEN); // Trạng thái hợp lệ để hủy

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId(sessionId);
        request.setKey(validOTP);

        // Giả lập hành vi của DAO
        when(mockUserDAO.findById(adminId)).thenReturn(mockAdmin);
        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(mockSession);

        // 2. Act (Thực thi)
        boolean result = adminService.cancelSession(request, adminId);

        // 3. Assert (Kiểm chứng)
        assertTrue(result, "Hàm phải trả về true khi hủy thành công");

        // KIỂM CHỨNG DUY NHẤT CẦN THIẾT: AdminService đã gọi sang InMemoryBidService
        verify(mockInMemoryBidService, times(1)).forceCancelSession(sessionId);

        // Tuyệt đối KHÔNG verify mockScheduler, mockCacheManager, mockBroadcastService ở đây
    }

    @Test
    @DisplayName("Cancel Session: Thất bại khi cố hủy phiên đã FINISHED")
    void testCancelSession_Fail_AlreadyFinished() {
        Admin mockAdmin = new Admin();
        mockAdmin.setSecretKey("111111");

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.FINISHED); // Phiên đã kết thúc

        when(mockUserDAO.findById("admin-1")).thenReturn(mockAdmin);
        when(mockSessionDAO.getSessionById("session-1")).thenReturn(mockSession);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId("session-1");
        request.setKey("111111");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminService.cancelSession(request, "admin-1"));

        assertTrue(exception.getMessage().contains("đã kết thúc hoặc đã bị hủy"));
    }
}