package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.AdminActionRequestDTO;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.Admin;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Bidder;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

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

    @BeforeEach
    void setUp() throws Exception {
        adminService = AdminService.getInstance();

        // Sử dụng Reflection để tiêm các Mock vào các class Singleton
        injectSingleton(UserDAO.class, mockUserDAO);
        injectSingleton(AuctionSessionDAO.class, mockSessionDAO);
        injectSingleton(AuctionServer.class, mockAuctionServer);
        injectSingleton(SearchCacheManager.class, mockCacheManager);
        injectSingleton(RealtimeBroadcastService.class, mockBroadcastService);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Trả lại trạng thái sạch cho các Singleton sau mỗi bài test
        injectSingleton(UserDAO.class, null);
        injectSingleton(AuctionSessionDAO.class, null);
        injectSingleton(AuctionServer.class, null);
        injectSingleton(SearchCacheManager.class, null);
        injectSingleton(RealtimeBroadcastService.class, null);
    }

    // --- Helper Method ---
    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
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
        mockAdmin.setOtpSecretKey(validOTP);

        User mockTarget = new Bidder();
        mockTarget.setId(targetId);
        mockTarget.setRole(RoleType.BIDDER);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId(targetId);
        request.setOtpCode(validOTP);

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
    }

    @Test
    @DisplayName("Ban User: Thất bại và bắn lỗi khi nhập sai OTP")
    void testBanUser_Fail_WrongOTP() {
        Admin mockAdmin = new Admin();
        mockAdmin.setOtpSecretKey("123456"); // OTP đúng

        when(mockUserDAO.findById("admin-1")).thenReturn(mockAdmin);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId("bidder-1");
        request.setOtpCode("999999"); // Cố tình nhập sai

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
        // 1. Arrange
        String adminId = "admin-1";
        String sessionId = "session-1";
        String validOTP = "654321";

        Admin mockAdmin = new Admin();
        mockAdmin.setOtpSecretKey(validOTP);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setStatus(SessionStatus.OPEN);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId(sessionId);
        request.setOtpCode(validOTP);
        request.setActionReason("Vi phạm bản quyền");

        when(mockUserDAO.findById(adminId)).thenReturn(mockAdmin);
        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(mockSession);
        when(mockSessionDAO.updateStatus(sessionId, SessionStatus.CANCELED)).thenReturn(true);

        // 2. Act
        boolean result = adminService.cancelSession(request, adminId);

        // 3. Assert
        assertTrue(result);
        verify(mockCacheManager, times(1)).removeSession(sessionId); // Kiểm tra dọn dẹp RAM
        verify(mockBroadcastService, times(1)).broadcast(eq(sessionId), any()); // Kiểm tra phát thanh
    }

    @Test
    @DisplayName("Cancel Session: Thất bại khi cố hủy phiên đã FINISHED")
    void testCancelSession_Fail_AlreadyFinished() {
        Admin mockAdmin = new Admin();
        mockAdmin.setOtpSecretKey("111111");

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.FINISHED); // Phiên đã kết thúc

        when(mockUserDAO.findById("admin-1")).thenReturn(mockAdmin);
        when(mockSessionDAO.getSessionById("session-1")).thenReturn(mockSession);

        AdminActionRequestDTO request = new AdminActionRequestDTO();
        request.setTargetId("session-1");
        request.setOtpCode("111111");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminService.cancelSession(request, "admin-1"));

        assertTrue(exception.getMessage().contains("đã kết thúc hoặc đã bị hủy"));
    }
}