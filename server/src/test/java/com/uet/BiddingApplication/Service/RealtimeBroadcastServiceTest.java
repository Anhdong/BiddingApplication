package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import com.uet.BiddingApplication.ServerClass.ClientConnectionHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RealtimeBroadcastServiceTest {

    private RealtimeBroadcastService realtimeService;

    // Khai báo Mock cho Server và đường ống kết nối của Client
    @Mock private AuctionServer mockAuctionServer;
    @Mock private ClientConnectionHandler mockHandlerUser1;
    @Mock private ClientConnectionHandler mockHandlerUser2;

    @BeforeEach
    void setUp() throws Exception {
        realtimeService = RealtimeBroadcastService.getInstance();

        // Tiêm Mock AuctionServer vào Singleton để không gọi Socket thật
        injectSingleton(AuctionServer.class, mockAuctionServer);

        // Dọn dẹp phòng (Map) trước mỗi test case để tránh dữ liệu rác
        clearRoomSubscribers();
    }

    @AfterEach
    void tearDown() throws Exception {
        injectSingleton(AuctionServer.class, null);
        clearRoomSubscribers();
    }

    // --- Helper Methods (Reflection) ---
    private void injectSingleton(Class<?> clazz, Object mockInstance) throws Exception {
        Field field = clazz.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mockInstance);
    }

    private void clearRoomSubscribers() throws Exception {
        Field field = RealtimeBroadcastService.class.getDeclaredField("roomSubscribers");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Set<String>> map = (ConcurrentHashMap<String, Set<String>>) field.get(realtimeService);
        map.clear();
    }

    private boolean isUserInRoom(String sessionId, String userId) throws Exception {
        Field field = RealtimeBroadcastService.class.getDeclaredField("roomSubscribers");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Set<String>> map = (ConcurrentHashMap<String, Set<String>>) field.get(realtimeService);
        Set<String> audience = map.get(sessionId);
        return audience != null && audience.contains(userId);
    }

    private boolean isRoomExists(String sessionId) throws Exception {
        Field field = RealtimeBroadcastService.class.getDeclaredField("roomSubscribers");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Set<String>> map = (ConcurrentHashMap<String, Set<String>>) field.get(realtimeService);
        return map.containsKey(sessionId);
    }

    // ==========================================
    // TEST CASES: subscribe
    // ==========================================

    @Test
    @DisplayName("subscribe: Thành công thêm User vào phòng")
    void testSubscribe_Success() throws Exception {
        String sessionId = "room-1";
        String userId = "user-1";

        realtimeService.subscribe(sessionId, userId);

        assertTrue(isUserInRoom(sessionId, userId), "User phải được thêm vào đúng phòng");
    }

    @Test
    @DisplayName("subscribe: Fail-Fast ném lỗi khi dữ liệu đầu vào null hoặc rỗng")
    void testSubscribe_Fail_InvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> realtimeService.subscribe(null, "user-1"));
        assertThrows(IllegalArgumentException.class, () -> realtimeService.subscribe("", "user-1"));
        assertThrows(IllegalArgumentException.class, () -> realtimeService.subscribe("room-1", null));
        assertThrows(IllegalArgumentException.class, () -> realtimeService.subscribe("room-1", "   "));
    }

    // ==========================================
    // TEST CASES: unsubscribe & unsubscribeFromAll
    // ==========================================

    @Test
    @DisplayName("unsubscribe: Rút User khỏi phòng và xóa luôn phòng nếu trống")
    void testUnsubscribe_RemoveUserAndCleanEmptyRoom() throws Exception {
        String sessionId = "room-1";
        String userId = "user-1";

        // Thêm vào trước
        realtimeService.subscribe(sessionId, userId);
        assertTrue(isUserInRoom(sessionId, userId));

        // Rút ra
        realtimeService.unsubscribe(sessionId, userId);

        assertFalse(isUserInRoom(sessionId, userId));
        assertFalse(isRoomExists(sessionId), "Phòng trống thì phải bị remove khỏi ConcurrentHashMap");
    }

    @Test
    @DisplayName("unsubscribeFromAll: Dọn dẹp sạch sẽ User khỏi mọi phòng khi thoát app")
    void testUnsubscribeFromAll_Success() throws Exception {
        String userId = "user-1";
        realtimeService.subscribe("room-1", userId);
        realtimeService.subscribe("room-2", userId);
        realtimeService.subscribe("room-1", "user-2"); // user-2 ở lại room-1

        realtimeService.unsubscribeFromAll(userId);

        assertFalse(isUserInRoom("room-1", userId));
        assertFalse(isUserInRoom("room-2", userId));
        assertTrue(isUserInRoom("room-1", "user-2"), "Không được xóa nhầm người khác");
        assertFalse(isRoomExists("room-2"), "room-2 trống phải bị xóa");
    }

    // ==========================================
    // TEST CASES: broadcast & sendPrivateMessage
    // ==========================================

    @Test
    @DisplayName("broadcast: Phát tin thành công cho tất cả khán giả đang online")
    void testBroadcast_Success() {
        String sessionId = "room-1";
        realtimeService.subscribe(sessionId, "user-1");
        realtimeService.subscribe(sessionId, "user-2");
        realtimeService.subscribe(sessionId, "user-offline");

        ResponsePacket<String> mockPacket = new ResponsePacket<>();

        // Giả lập Server tìm thấy Handler của user-1 và user-2, nhưng user-offline thì null (rớt mạng)
        when(mockAuctionServer.getClientHandler("user-1")).thenReturn(mockHandlerUser1);
        when(mockAuctionServer.getClientHandler("user-2")).thenReturn(mockHandlerUser2);
        when(mockAuctionServer.getClientHandler("user-offline")).thenReturn(null);

        realtimeService.broadcast(sessionId, mockPacket);

        // Xác minh chỉ những user online mới được gọi hàm sendPacket
        verify(mockHandlerUser1, times(1)).sendPacket(mockPacket);
        verify(mockHandlerUser2, times(1)).sendPacket(mockPacket);
    }

    @Test
    @DisplayName("sendPrivateMessage: Chỉ phát cho đúng 1 người")
    void testSendPrivateMessage_Success() {
        String userId = "user-1";
        ResponsePacket<String> mockPacket = new ResponsePacket<>();

        when(mockAuctionServer.getClientHandler(userId)).thenReturn(mockHandlerUser1);

        realtimeService.sendPrivateMessage(userId, mockPacket);

        verify(mockHandlerUser1, times(1)).sendPacket(mockPacket);
    }

    // ==========================================
    // TEST CASES: closeRoom
    // ==========================================

    @Test
    @DisplayName("closeRoom: Xóa ngay lập tức cả phòng khỏi bộ nhớ")
    void testCloseRoom_Success() throws Exception {
        String sessionId = "room-1";
        realtimeService.subscribe(sessionId, "user-1");
        realtimeService.subscribe(sessionId, "user-2");

        realtimeService.closeRoom(sessionId);

        assertFalse(isRoomExists(sessionId), "Phòng phải bị xóa hoàn toàn");
    }
}