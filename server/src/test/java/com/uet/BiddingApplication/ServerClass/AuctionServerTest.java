package com.uet.BiddingApplication.ServerClass;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite cho AuctionServer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionServer Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionServerTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field f = AuctionServer.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @AfterEach
    void stopServer() {
        try { AuctionServer.getInstance().stop(); } catch (Exception ignored) {}
    }

    // ----------------------------------------------------------------
    //  TC-AS-01  Singleton trả về cùng một instance
    // ----------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("TC-AS-01: getInstance() luôn trả về cùng một đối tượng")
    void testSingletonReturnsSameInstance() {
        assertSame(AuctionServer.getInstance(), AuctionServer.getInstance(),
                "Hai lần gọi getInstance() phải trả về cùng một tham chiếu");
    }

    // ----------------------------------------------------------------
    //  TC-AS-02  Singleton thread-safe
    // ----------------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("TC-AS-02: getInstance() thread-safe khi 50 luồng cùng gọi")
    void testSingletonThreadSafe() throws InterruptedException {
        int n = 50;
        AuctionServer[] results = new AuctionServer[n];
        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            threads[idx] = new Thread(() -> results[idx] = AuctionServer.getInstance());
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        AuctionServer expected = results[0];
        for (AuctionServer as : results)
            assertSame(expected, as, "Tất cả luồng phải nhận cùng một instance");
    }

    // ----------------------------------------------------------------
    //  TC-AS-03  registerClient() lưu handler đúng
    // ----------------------------------------------------------------
    @Test
    @Order(3)
    @DisplayName("TC-AS-03: registerClient() lưu handler và getClientHandler() trả về đúng")
    void testRegisterClient() {
        AuctionServer server = AuctionServer.getInstance();
        ClientConnectionHandler mockHandler = mock(ClientConnectionHandler.class);

        server.registerClient("user-001", mockHandler);

        assertSame(mockHandler, server.getClientHandler("user-001"),
                "getClientHandler() phải trả về đúng handler đã đăng ký");
    }

    // ----------------------------------------------------------------
    //  TC-AS-04  ĐOẠN SỬA DUY NHẤT: Bắt lỗi NullPointerException của ConcurrentHashMap
    // ----------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("TC-AS-04: registerClient(null, handler) không lỗi nhưng getClientHandler(null) phải ném NPE")
    void testRegisterClientNullUserId() {
        AuctionServer server = AuctionServer.getInstance();
        ClientConnectionHandler mockHandler = mock(ClientConnectionHandler.class);

        assertDoesNotThrow(() -> server.registerClient(null, mockHandler),
                "registerClient với userId null không được ném exception");


        assertThrows(NullPointerException.class, () -> server.getClientHandler(null),
                "getClientHandler(null) bắt buộc phải ném lỗi NPE của ConcurrentHashMap");
    }

    // ----------------------------------------------------------------
    //  TC-AS-05  registerClient() với handler null – bỏ qua, không crash
    // ----------------------------------------------------------------
    @Test
    @Order(5)
    @DisplayName("TC-AS-05: registerClient(userId, null) không ném exception")
    void testRegisterClientNullHandler() {
        AuctionServer server = AuctionServer.getInstance();
        assertDoesNotThrow(() -> server.registerClient("user-002", null),
                "registerClient với handler null không được ném exception");
    }

    // ----------------------------------------------------------------
    //  TC-AS-06  unregisterClient() xóa đúng client khỏi map
    // ----------------------------------------------------------------
    @Test
    @Order(6)
    @DisplayName("TC-AS-06: unregisterClient() xóa client khỏi map")
    void testUnregisterClient() {
        AuctionServer server = AuctionServer.getInstance();
        ClientConnectionHandler mockHandler = mock(ClientConnectionHandler.class);

        server.registerClient("user-003", mockHandler);
        assertNotNull(server.getClientHandler("user-003"));

        server.unregisterClient("user-003");
        assertNull(server.getClientHandler("user-003"),
                "Sau unregisterClient(), getClientHandler() phải trả về null");
    }

    // ----------------------------------------------------------------
    //  TC-AS-07  unregisterClient() với userId không tồn tại – không crash
    // ----------------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("TC-AS-07: unregisterClient() với userId chưa đăng ký không ném exception")
    void testUnregisterNonExistentClient() {
        assertDoesNotThrow(() -> AuctionServer.getInstance().unregisterClient("ghost-user"),
                "unregisterClient() với user chưa tồn tại không được ném exception");
    }

    // ----------------------------------------------------------------
    //  TC-AS-08  unregisterClient(null) – không crash
    // ----------------------------------------------------------------
    @Test
    @Order(8)
    @DisplayName("TC-AS-08: unregisterClient(null) không ném exception")
    void testUnregisterNullUserId() {
        assertDoesNotThrow(() -> AuctionServer.getInstance().unregisterClient(null),
                "unregisterClient(null) không được ném exception");
    }

    // ----------------------------------------------------------------
    //  TC-AS-09  kickUser() gọi closeConnection() trên handler và xóa khỏi map
    // ----------------------------------------------------------------
    @Test
    @Order(9)
    @DisplayName("TC-AS-09: kickUser() gọi closeConnection() và xóa client khỏi map")
    void testKickUser() {
        AuctionServer server = AuctionServer.getInstance();
        ClientConnectionHandler mockHandler = mock(ClientConnectionHandler.class);
        doNothing().when(mockHandler).closeConnection();

        server.registerClient("user-kick", mockHandler);
        server.kickUser("user-kick");

        verify(mockHandler, times(1)).closeConnection();
        assertNull(server.getClientHandler("user-kick"),
                "Sau kickUser(), client phải bị xóa khỏi map");
    }

    // ----------------------------------------------------------------
    //  TC-AS-10  kickUser() với userId không tồn tại – không crash
    // ----------------------------------------------------------------
    @Test
    @Order(10)
    @DisplayName("TC-AS-10: kickUser() với userId không tồn tại không ném exception")
    void testKickNonExistentUser() {
        assertDoesNotThrow(() -> AuctionServer.getInstance().kickUser("no-such-user"),
                "kickUser() với user không tồn tại không được ném exception");
    }

    // ----------------------------------------------------------------
    //  TC-AS-11  kickUser(null) – không crash
    // ----------------------------------------------------------------
    @Test
    @Order(11)
    @DisplayName("TC-AS-11: kickUser(null) không ném exception")
    void testKickNullUser() {
        assertDoesNotThrow(() -> AuctionServer.getInstance().kickUser(null),
                "kickUser(null) không được ném exception");
    }

    // ----------------------------------------------------------------
    //  TC-AS-12  getClientHandler() trả về null khi chưa register
    // ----------------------------------------------------------------
    @Test
    @Order(12)
    @DisplayName("TC-AS-12: getClientHandler() trả về null khi user chưa đăng ký")
    void testGetHandlerNotRegistered() {
        assertNull(AuctionServer.getInstance().getClientHandler("unknown-user"),
                "getClientHandler() phải trả về null khi user chưa đăng ký");
    }

    // ----------------------------------------------------------------
    //  TC-AS-13  stop() gọi closeConnection() trên tất cả client
    // ----------------------------------------------------------------
    @Test
    @Order(13)
    @DisplayName("TC-AS-13: stop() gọi closeConnection() trên tất cả client đang kết nối")
    void testStopClosesAllClients() {
        AuctionServer server = AuctionServer.getInstance();
        ClientConnectionHandler handler1 = mock(ClientConnectionHandler.class);
        ClientConnectionHandler handler2 = mock(ClientConnectionHandler.class);
        doNothing().when(handler1).closeConnection();
        doNothing().when(handler2).closeConnection();

        server.registerClient("user-A", handler1);
        server.registerClient("user-B", handler2);

        server.stop();

        verify(handler1, times(1)).closeConnection();
        verify(handler2, times(1)).closeConnection();
    }

    // ----------------------------------------------------------------
    //  TC-AS-14  Đăng ký nhiều client, kiểm tra map đúng kích thước
    // ----------------------------------------------------------------
    @Test
    @Order(14)
    @DisplayName("TC-AS-14: Đăng ký 3 client khác nhau, getClientHandler() trả về đúng từng client")
    void testRegisterMultipleClients() {
        AuctionServer server = AuctionServer.getInstance();
        ClientConnectionHandler h1 = mock(ClientConnectionHandler.class);
        ClientConnectionHandler h2 = mock(ClientConnectionHandler.class);
        ClientConnectionHandler h3 = mock(ClientConnectionHandler.class);

        server.registerClient("u1", h1);
        server.registerClient("u2", h2);
        server.registerClient("u3", h3);

        assertSame(h1, server.getClientHandler("u1"));
        assertSame(h2, server.getClientHandler("u2"));
        assertSame(h3, server.getClientHandler("u3"));
    }
}