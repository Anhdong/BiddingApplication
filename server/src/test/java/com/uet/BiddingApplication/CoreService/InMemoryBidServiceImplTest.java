package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;
import com.uet.BiddingApplication.Enum.BidType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Service.RealtimeBroadcastService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InMemoryBidServiceImplTest {

    private InMemoryBidServiceImpl bidService;

    @Mock private SearchCacheManager mockCache;
    @Mock private AuctionSessionDAO mockSessionDAO;
    @Mock private BidDAO mockBidDAO;
    @Mock private RealtimeBroadcastService mockBroadcast;

    @BeforeEach
    void setUp() throws Exception {
        bidService = InMemoryBidServiceImpl.getInstance();

        // Inject các Mock vào Service qua Reflection
        injectField(bidService, "searchCacheManager", mockCache);
        injectField(bidService, "realtimeBroadcastService", mockBroadcast);

        // Inject Mock vào các DAO Singleton
        injectPrivateField(bidService, "bidDAO", mockBidDAO);

        injectStaticField(BidDAO.class, "instance", mockBidDAO);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
        System.out.println("✅ Đã tiêm Mock vào thuộc tính: " + fieldName + " của Service");
    }
    private void injectStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
        System.out.println("✅ Đã tiêm Mock vào Static Field: " + fieldName + " của " + clazz.getSimpleName());
    }


    @Test
    @DisplayName("Validate Bid: Giá đặt phải lớn hơn giá hiện tại + bước giá")
    void testValidateBid() {
        AuctionSession session = new AuctionSession();
        session.setCurrentPrice(new BigDecimal("100"));
        session.setBidStep(new BigDecimal("10"));
        session.setEndTime(LocalDateTime.now().plusHours(1));

        // Giá đặt 105 (Sai: 100 + 10 = 110 mới đúng)
        assertFalse(bidService.validateBid(new BigDecimal("105"), session));

        // Giá đặt 110 (Đúng)
        assertTrue(bidService.validateBid(new BigDecimal("110"), session));
    }

    @Test
    @DisplayName("Luồng đặt giá: Kiểm tra gọi Transaction xuống Database")
    void testPlaceBidAtomicTransaction_Called() throws InterruptedException {
        // 1. Chuẩn bị dữ liệu
        String sessionId = UUID.randomUUID().toString();
        String bidderId = UUID.randomUUID().toString();
        BigDecimal bidAmount = new BigDecimal("2000.00");

        AuctionSession session = new AuctionSession();
        session.setId(sessionId);
        session.setStatus(SessionStatus.RUNNING);
        session.setCurrentPrice(new BigDecimal("1000.00"));
        session.setBidStep(new BigDecimal("100.00"));
        session.setEndTime(LocalDateTime.now().plusHours(1));

        // Giả lập Cache trả về phiên đang chạy
        when(mockCache.getSession(sessionId)).thenReturn(session);

        // 2. STUB: Bắt buộc phải trả về true để logic đi tiếp vào nhánh Broadcast
        when(mockBidDAO.placeBidAtomicTransaction(
                eq(sessionId),
                eq(bidderId),
                any(BigDecimal.class),
                any(BidType.class)
        )).thenReturn(true);

        // 3. Thực thi
        BidRequestDTO request = new BidRequestDTO();
        request.setSessionId(sessionId);
        request.setBidAmount(bidAmount);
        request.setBidType(BidType.MANUAL);

        bidService.enqueueBid(request, bidderId);

        // 4. KIỂM THỬ BẤT ĐỒNG BỘ
        // Thay vì Thread.sleep(500), ta có thể dùng verify với timeout (tối ưu hơn)
        verify(mockBidDAO, timeout(10000).atLeastOnce()).placeBidAtomicTransaction(
                eq(sessionId),
                eq(bidderId),
                any(BigDecimal.class), // Dùng any để tránh lỗi so sánh BigDecimal scale
                eq(BidType.MANUAL)
        );

        // Kiểm tra xem có broadcast không (chỉ khi dbSuccess = true mới có)
        verify(mockBroadcast, timeout(1000)).broadcast(eq(sessionId), any());
    }
}