package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.InMemoryBidServiceImpl;
import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.AutoBidSetting;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoBidManagerTest {

    private AutoBidManager autoBidManager;

    // 1. Khai báo các Mock để cô lập AutoBidManager khỏi các hệ thống khác
    @Mock private SearchCacheManager mockCacheManager;
    @Mock private RealtimeBroadcastService mockBroadcastService;
    @Mock private InMemoryBidServiceImpl mockBidService;

    @BeforeEach
    void setUp() throws Exception {
        autoBidManager = AutoBidManager.getInstance();

        // 2. Tiêm Mock vào các Singleton bằng Reflection
        injectSingleton(SearchCacheManager.class, mockCacheManager);
        injectSingleton(RealtimeBroadcastService.class, mockBroadcastService);
        injectSingleton(InMemoryBidServiceImpl.class, mockBidService);

        // 3. Xóa sạch dữ liệu hàng đợi RAM trước mỗi test case để tránh Flaky Tests
        clearAutoBidQueues();
    }

    @AfterEach
    void tearDown() throws Exception {
        injectSingleton(SearchCacheManager.class, null);
        injectSingleton(RealtimeBroadcastService.class, null);
        injectSingleton(InMemoryBidServiceImpl.class, null);
        clearAutoBidQueues();
    }

    // --- Helper Methods (Reflection) ---
    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    private void clearAutoBidQueues() throws Exception {
        Field field = AutoBidManager.class.getDeclaredField("autoBidQueues");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ConcurrentLinkedQueue<AutoBidSetting>> map =
                (ConcurrentHashMap<String, ConcurrentLinkedQueue<AutoBidSetting>>) field.get(autoBidManager);
        map.clear();
    }

    private int getQueueSize(String sessionId) throws Exception {
        Field field = AutoBidManager.class.getDeclaredField("autoBidQueues");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ConcurrentLinkedQueue<AutoBidSetting>> map =
                (ConcurrentHashMap<String, ConcurrentLinkedQueue<AutoBidSetting>>) field.get(autoBidManager);
        ConcurrentLinkedQueue<AutoBidSetting> queue = map.get(sessionId);
        return queue != null ? queue.size() : 0;
    }

    // ==========================================
    // TEST CASES: registerAutoBid & cancelAutoBid
    // ==========================================

    @Test
    @DisplayName("registerAutoBid: Thêm mới thành công và ghi đè nếu đăng ký lại")
    void testRegisterAutoBid_SuccessAndOverride() throws Exception {
        String sessionId = "session-1";
        String bidderId = "bidder-1";

        AutoBidSetting setting1 = new AutoBidSetting();
        setting1.setSessionId(sessionId);
        setting1.setBidderId(bidderId);
        setting1.setMaxBid(new BigDecimal("1000"));

        AutoBidSetting setting2 = new AutoBidSetting();
        setting2.setSessionId(sessionId);
        setting2.setBidderId(bidderId);
        setting2.setMaxBid(new BigDecimal("2000")); // Cài đặt mới của cùng 1 người

        // Đăng ký lần 1
        autoBidManager.registerAutoBid(setting1);
        assertEquals(1, getQueueSize(sessionId));

        // Đăng ký lần 2 (Phải xóa cái cũ và cập nhật cái mới)
        autoBidManager.registerAutoBid(setting2);
        assertEquals(1, getQueueSize(sessionId), "Không được phép có 2 cài đặt của cùng 1 user");
    }

    @Test
    @DisplayName("cancelAutoBid: Thành công hủy cài đặt và xóa phòng nếu trống")
    void testCancelAutoBid_Success() throws Exception {
        String sessionId = "session-1";

        AutoBidSetting setting = new AutoBidSetting();
        setting.setSessionId(sessionId);
        setting.setBidderId("bidder-1");

        autoBidManager.registerAutoBid(setting);
        assertEquals(1, getQueueSize(sessionId));

        // Hủy đăng ký
        autoBidManager.cancelAutoBid(sessionId, "bidder-1");
        assertEquals(0, getQueueSize(sessionId));
    }

    // ==========================================
    // TEST CASES: triggerAutoBid (Logic Đấu giá Tự động)
    // ==========================================

    @Test
    @DisplayName("triggerAutoBid: Bỏ qua (continue) nếu đang là người dẫn đầu")
    void testTriggerAutoBid_SkipHighestBidder() throws Exception {
        String sessionId = "session-1";
        String bidderId = "bidder-1";

        AutoBidSetting setting = new AutoBidSetting();
        setting.setSessionId(sessionId);
        setting.setBidderId(bidderId);
        autoBidManager.registerAutoBid(setting);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setBidStep(new BigDecimal("10"));

        when(mockCacheManager.getSession(sessionId)).thenReturn(mockSession);

        // Kích hoạt nhưng truyền highestBidderId chính là người này
        autoBidManager.triggerAutoBid(sessionId, new BigDecimal("100"), bidderId);

        // Xác minh không có lệnh bid nào được đẩy đi
        verify(mockBidService, never()).enqueueBid(any(), anyString());
        // Xác minh không bị xóa khỏi hàng đợi
        assertEquals(1, getQueueSize(sessionId));
    }

    @Test
    @DisplayName("triggerAutoBid: Xóa khỏi hàng đợi và gửi tin nhắn khi chạm ngưỡng Max Bid")
    void testTriggerAutoBid_RemoveWhenMaxBidExceeded() throws Exception {
        String sessionId = "session-1";
        String bidderId = "bidder-1";

        AutoBidSetting setting = new AutoBidSetting();
        setting.setSessionId(sessionId);
        setting.setBidderId(bidderId);
        setting.setIncrement(new BigDecimal("10"));
        setting.setMaxBid(new BigDecimal("105")); // Max bid là 105

        autoBidManager.registerAutoBid(setting);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setBidStep(new BigDecimal("10"));
        when(mockCacheManager.getSession(sessionId)).thenReturn(mockSession);

        // Giá hiện tại là 100, bước giá 10 -> Giá tiếp theo phải là 110. (110 > 105 nên sẽ thất bại)
        autoBidManager.triggerAutoBid(sessionId, new BigDecimal("100"), "another-bidder");

        // Xác minh user đã bị xóa khỏi Queue (bị loại)
        assertEquals(0, getQueueSize(sessionId));

        // Xác minh đã gửi tin nhắn riêng báo lỗi
        verify(mockBroadcastService, times(1)).sendPrivateMessage(eq(bidderId), any(ResponsePacket.class));

        // Xác minh không có lệnh bid nào được đẩy đi
        verify(mockBidService, never()).enqueueBid(any(), anyString());
    }

    @Test
    @DisplayName("triggerAutoBid: Gửi lệnh hợp lệ và Lập tức nhường luồng (Break)")
    void testTriggerAutoBid_SuccessAndBreak() throws Exception {
        String sessionId = "session-1";

        // Tạo 2 user cùng đăng ký Auto-bid
        AutoBidSetting setting1 = new AutoBidSetting();
        setting1.setSessionId(sessionId);
        setting1.setBidderId("bidder-1");
        setting1.setIncrement(new BigDecimal("10"));
        setting1.setMaxBid(new BigDecimal("5000")); // Tiền rủng rỉnh

        AutoBidSetting setting2 = new AutoBidSetting();
        setting2.setSessionId(sessionId);
        setting2.setBidderId("bidder-2");
        setting2.setIncrement(new BigDecimal("15"));
        setting2.setMaxBid(new BigDecimal("5000"));

        // Do dùng FIFO, bidder-1 vào trước sẽ được xử lý trước
        autoBidManager.registerAutoBid(setting1);
        autoBidManager.registerAutoBid(setting2);

        AuctionSession mockSession = new AuctionSession();
        mockSession.setBidStep(new BigDecimal("5")); // Bước giá hệ thống là 5
        when(mockCacheManager.getSession(sessionId)).thenReturn(mockSession);

        // Kích hoạt
        autoBidManager.triggerAutoBid(sessionId, new BigDecimal("100"), "another-bidder");

        // Bắt lấy gói tin BidRequestDTO được đẩy sang Core
        ArgumentCaptor<BidRequestDTO> captor = ArgumentCaptor.forClass(BidRequestDTO.class);

        // Xác minh Core Service CÓ NHẬN ĐƯỢC ĐÚNG 1 LỆNH (Kiểm chứng từ khóa 'break')
        verify(mockBidService, times(1)).enqueueBid(captor.capture(), eq("bidder-1"));

        // Xác minh giá trị gửi đi: Giá hiện tại 100 + increment của user (10) = 110
        assertEquals(new BigDecimal("110"), captor.getValue().getBidAmount());

        // Xác minh cả 2 user VẪN CÒN TRONG HÀNG ĐỢI chờ cho vòng tiếp theo
        assertEquals(2, getQueueSize(sessionId));
    }
}