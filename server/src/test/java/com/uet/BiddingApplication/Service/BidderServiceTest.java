package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Impl.SessionRegistrationDAO;
import com.uet.BiddingApplication.DTO.Request.SessionRegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.SessionRegistration;
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
public class BidderServiceTest {

    private BidderService bidderService;

    @Mock private SessionRegistrationDAO mockRegDAO;
    @Mock private BidDAO mockBidDAO;
    @Mock private AuctionSessionDAO mockSessionDAO;

    @BeforeEach
    void setUp() throws Exception {
        bidderService = BidderService.getInstance();
        injectSingleton(SessionRegistrationDAO.class, mockRegDAO);
        injectSingleton(BidDAO.class, mockBidDAO);
        injectSingleton(AuctionSessionDAO.class, mockSessionDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        injectSingleton(SessionRegistrationDAO.class, null);
        injectSingleton(BidDAO.class, null);
        injectSingleton(AuctionSessionDAO.class, null);
    }

    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    // ==========================================
    // TEST CASES: getRegisteredSessions
    // ==========================================

    @Test
    @DisplayName("getRegisteredSessions: Thành công trả về danh sách phiên")
    void testGetRegisteredSessions_Success() {
        String bidderId = "bidder-1";
        List<AuctionCardDTO> mockList = Arrays.asList(new AuctionCardDTO(), new AuctionCardDTO());
        when(mockRegDAO.getRegisteredSessions(bidderId)).thenReturn(mockList);

        List<AuctionCardDTO> result = bidderService.getRegisteredSessions(bidderId);

        assertEquals(2, result.size(), "Phải trả về đúng số lượng phiên đã đăng ký");
        verify(mockRegDAO, times(1)).getRegisteredSessions(bidderId);
    }

    @Test
    @DisplayName("getRegisteredSessions: Thất bại do BidderID trống")
    void testGetRegisteredSessions_Fail_EmptyId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bidderService.getRegisteredSessions(""));
        assertEquals("Mã người dùng (Bidder ID) không được để trống.", exception.getMessage());
    }

    // ==========================================
    // TEST CASES: getBidderHistory
    // ==========================================

    @Test
    @DisplayName("getBidderHistory: Thành công trả về lịch sử trả giá")
    void testGetBidderHistory_Success() {
        String bidderId = "bidder-1";
        // Tối ưu hóa: Dùng singletonList thay cho asList khi chỉ có 1 phần tử
        List<BidderHistoryResponseDTO> mockHistory = Collections.singletonList(new BidderHistoryResponseDTO());
        when(mockBidDAO.getBidderHistory(bidderId)).thenReturn(mockHistory);

        List<BidderHistoryResponseDTO> result = bidderService.getBidderHistory(bidderId);

        assertFalse(result.isEmpty(), "Danh sách lịch sử không được rỗng");
        verify(mockBidDAO, times(1)).getBidderHistory(bidderId);
    }

    // ==========================================
    // TEST CASES: registerSession
    // ==========================================

    @Test
    @DisplayName("registerSession: Thành công đăng ký phiên đấu giá")
    void testRegisterSession_Success() {
        String bidderId = "bidder-1";
        String sessionId = "session-1";

        // SỬA LỖI COMPILER: Sử dụng constructor có tham số
        SessionRegisterRequestDTO request = new SessionRegisterRequestDTO(sessionId);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setId(sessionId);
        mockSession.setStatus(SessionStatus.OPEN);

        when(mockSessionDAO.getSessionById(sessionId)).thenReturn(mockSession);
        when(mockRegDAO.checkRegistration(bidderId, sessionId)).thenReturn(false);
        when(mockRegDAO.registerBidder(any(SessionRegistration.class))).thenReturn(true);

        boolean result = bidderService.registerSession(request, bidderId);

        assertTrue(result, "Hàm phải trả về true khi đăng ký thành công");
        verify(mockRegDAO, times(1)).registerBidder(any(SessionRegistration.class));
    }

    @Test
    @DisplayName("registerSession: Thất bại do phiên không ở trạng thái OPEN")
    void testRegisterSession_Fail_SessionNotOpen() {
        String bidderId = "bidder-1";
        SessionRegisterRequestDTO request = new SessionRegisterRequestDTO("session-1");

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.RUNNING);

        when(mockSessionDAO.getSessionById("session-1")).thenReturn(mockSession);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bidderService.registerSession(request, bidderId));

        assertTrue(exception.getMessage().contains("không ở trạng thái mở đăng ký"));
        verify(mockRegDAO, never()).registerBidder(any());
    }

    @Test
    @DisplayName("registerSession: Thất bại do đã đăng ký từ trước")
    void testRegisterSession_Fail_AlreadyRegistered() {
        String bidderId = "bidder-1";
        SessionRegisterRequestDTO request = new SessionRegisterRequestDTO("session-1");

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.OPEN);

        when(mockSessionDAO.getSessionById("session-1")).thenReturn(mockSession);
        when(mockRegDAO.checkRegistration(bidderId, "session-1")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bidderService.registerSession(request, bidderId));

        assertEquals("Bạn đã đăng ký tham gia phiên đấu giá này từ trước rồi.", exception.getMessage());
    }

    @Test
    @DisplayName("registerSession: Thất bại do lỗi hệ thống (Database lỗi)")
    void testRegisterSession_Fail_DatabaseError() {
        String bidderId = "bidder-1";
        SessionRegisterRequestDTO request = new SessionRegisterRequestDTO("session-1");

        AuctionSession mockSession = new AuctionSession();
        mockSession.setStatus(SessionStatus.OPEN);

        when(mockSessionDAO.getSessionById("session-1")).thenReturn(mockSession);
        when(mockRegDAO.checkRegistration(bidderId, "session-1")).thenReturn(false);
        when(mockRegDAO.registerBidder(any(SessionRegistration.class))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bidderService.registerSession(request, bidderId));

        assertTrue(exception.getMessage().contains("Lỗi hệ thống"));
    }
}