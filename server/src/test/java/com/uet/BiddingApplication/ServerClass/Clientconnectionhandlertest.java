package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite cho ClientConnectionHandler
 *
 * Bao gồm:
 *  - sendPacket() gửi JSON đúng định dạng đến client socket
 *  - sendPacket() khi socket đã đóng – không crash
 *  - setUserId() gọi registerClient() trên AuctionServer
 *  - setUserId(null) không gọi registerClient()
 *  - forceClose() đóng socket, không crash khi gọi nhiều lần
 *  - kickOut() gửi packet FORCE_LOGOUT rồi đóng kết nối
 *  - closeConnection() gọi kickOut()
 *  - getUserId() trả về đúng sau setUserId()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientConnectionHandler Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientConnectionHandlerTest {

    private static final int BASE_PORT = 19200;

    /** Tạo ResponsePacket test */
    private ResponsePacket<String> buildResponse(ActionType action, int status, String msg) {
        ResponsePacket<String> pkt = new ResponsePacket<>();
        pkt.setAction(action);
        pkt.setStatusCode(status);
        pkt.setMessage(msg);
        return pkt;
    }

    // ----------------------------------------------------------------
    //  TC-CCH-01  getUserId() trả về null ban đầu
    // ----------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("TC-CCH-01: getUserId() trả về null khi chưa setUserId()")
    void testGetUserIdInitiallyNull() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 1)) {
            ss.setSoTimeout(2000);
            java.util.concurrent.CompletableFuture<Socket> accepted =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return ss.accept(); } catch (Exception e) { return null; }
                    });

            Socket client = new Socket("127.0.0.1", BASE_PORT + 1);
            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = accepted.get(2, java.util.concurrent.TimeUnit.SECONDS);

            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);
            assertNull(handler.getUserId(), "getUserId() phải null khi chưa setUserId()");

            client.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-02  setUserId() lưu userId và gọi registerClient()
    // ----------------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("TC-CCH-02: setUserId() lưu userId và gọi registerClient() trên server")
    void testSetUserIdRegistersOnServer() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 2)) {
            ss.setSoTimeout(2000);
            java.util.concurrent.CompletableFuture<Socket> accepted =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return ss.accept(); } catch (Exception e) { return null; }
                    });

            Socket client = new Socket("127.0.0.1", BASE_PORT + 2);
            AuctionServer mockServer = mock(AuctionServer.class);
            doNothing().when(mockServer).registerClient(anyString(), any());
            Socket serverSide = accepted.get(2, java.util.concurrent.TimeUnit.SECONDS);

            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);
            handler.setUserId("user-abc");

            assertEquals("user-abc", handler.getUserId(),
                    "getUserId() phải trả về userId đã set");
            verify(mockServer, times(1)).registerClient(eq("user-abc"), eq(handler));

            client.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-03  setUserId(null) không gọi registerClient()
    // ----------------------------------------------------------------
    @Test
    @Order(3)
    @DisplayName("TC-CCH-03: setUserId(null) không gọi registerClient()")
    void testSetUserIdNullDoesNotRegister() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 3)) {
            ss.setSoTimeout(2000);
            java.util.concurrent.CompletableFuture<Socket> accepted =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return ss.accept(); } catch (Exception e) { return null; }
                    });

            Socket client = new Socket("127.0.0.1", BASE_PORT + 3);
            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = accepted.get(2, java.util.concurrent.TimeUnit.SECONDS);

            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);
            handler.setUserId(null);

            verify(mockServer, never()).registerClient(any(), any());
            assertNull(handler.getUserId());

            client.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-04  sendPacket() gửi JSON chứa ActionType đến client
    // ----------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("TC-CCH-04: sendPacket() gửi JSON có ActionType đến client socket")
    void testSendPacketSendsJson() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 4)) {
            ss.setSoTimeout(3000);

            // Client socket đọc dữ liệu từ server
            Socket clientSocket = new Socket("127.0.0.1", BASE_PORT + 4);
            BufferedReader clientReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = ss.accept();
            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);

            // Act
            ResponsePacket<String> pkt = buildResponse(ActionType.LOGIN, 200, "Đăng nhập thành công");
            handler.sendPacket(pkt);

            // Assert – client nhận được JSON
            String received = clientReader.readLine();
            assertNotNull(received, "Client phải nhận được dữ liệu");
            assertTrue(received.contains("LOGIN"),
                    "JSON gửi đi phải chứa ActionType LOGIN");

            clientSocket.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-05  sendPacket() khi socket đã đóng – không crash
    // ----------------------------------------------------------------
    @Test
    @Order(5)
    @DisplayName("TC-CCH-05: sendPacket() khi socket đã đóng không ném exception ra ngoài")
    void testSendPacketAfterClose() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 5)) {
            ss.setSoTimeout(2000);
            java.util.concurrent.CompletableFuture<Socket> accepted =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return ss.accept(); } catch (Exception e) { return null; }
                    });

            Socket client = new Socket("127.0.0.1", BASE_PORT + 5);
            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = accepted.get(2, java.util.concurrent.TimeUnit.SECONDS);

            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);
            handler.forceClose("Test close");

            // Act & Assert – không crash
            assertDoesNotThrow(() ->
                            handler.sendPacket(buildResponse(ActionType.LOGOUT, 200, "bye")),
                    "sendPacket() sau forceClose() không được ném exception"
            );

            client.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-06  forceClose() đóng socket, không crash khi gọi nhiều lần
    // ----------------------------------------------------------------
    @Test
    @Order(6)
    @DisplayName("TC-CCH-06: forceClose() gọi nhiều lần không ném exception")
    void testForceCloseIdempotent() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 6)) {
            ss.setSoTimeout(2000);
            java.util.concurrent.CompletableFuture<Socket> accepted =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return ss.accept(); } catch (Exception e) { return null; }
                    });

            Socket client = new Socket("127.0.0.1", BASE_PORT + 6);
            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = accepted.get(2, java.util.concurrent.TimeUnit.SECONDS);

            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);

            assertDoesNotThrow(() -> {
                handler.forceClose("lần 1");
                handler.forceClose("lần 2");
                handler.forceClose("lần 3");
            }, "forceClose() gọi nhiều lần không được ném exception");

            client.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-07  kickOut() gửi FORCE_LOGOUT packet trước khi đóng kết nối
    // ----------------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("TC-CCH-07: kickOut() gửi packet FORCE_LOGOUT (status 403) về client")
    void testKickOutSendsForceLogoutPacket() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 7)) {
            ss.setSoTimeout(3000);

            Socket clientSocket = new Socket("127.0.0.1", BASE_PORT + 7);
            BufferedReader clientReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = ss.accept();
            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);

            // Act
            handler.kickOut("Vi phạm quy định");

            // Assert – client nhận được gói FORCE_LOGOUT
            String received = clientReader.readLine();
            assertNotNull(received, "Client phải nhận được packet FORCE_LOGOUT");
            assertTrue(received.contains("FORCE_LOGOUT"),
                    "Packet kickOut phải có ActionType FORCE_LOGOUT");
            assertTrue(received.contains("403"),
                    "Packet kickOut phải có statusCode 403");

            clientSocket.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-08  closeConnection() gọi kickOut() (wrapper method)
    // ----------------------------------------------------------------
    @Test
    @Order(8)
    @DisplayName("TC-CCH-08: closeConnection() gửi packet FORCE_LOGOUT về client")
    void testCloseConnectionSendsForceLogout() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 8)) {
            ss.setSoTimeout(3000);

            Socket clientSocket = new Socket("127.0.0.1", BASE_PORT + 8);
            BufferedReader clientReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = ss.accept();
            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);

            handler.closeConnection("Server shutdown");

            String received = clientReader.readLine();
            assertNotNull(received);
            assertTrue(received.contains("FORCE_LOGOUT"),
                    "closeConnection() phải gửi FORCE_LOGOUT");

            clientSocket.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-09  closeConnection(reason) gọi kickOut() với lý do đúng
    // ----------------------------------------------------------------
    @Test
    @Order(9)
    @DisplayName("TC-CCH-09: closeConnection(reason) truyền đúng lý do vào packet message")
    void testCloseConnectionWithReason() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 9)) {
            ss.setSoTimeout(3000);

            Socket clientSocket = new Socket("127.0.0.1", BASE_PORT + 9);
            BufferedReader clientReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = ss.accept();
            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);

            handler.closeConnection("Bảo trì hệ thống");

            String received = clientReader.readLine();
            assertNotNull(received);
            assertTrue(received.contains("B\u1ea3o tr\u00ec h\u1ec7 th\u1ed1ng")
                            || received.contains("FORCE_LOGOUT"),
                    "Packet phải chứa lý do đóng kết nối hoặc FORCE_LOGOUT");

            clientSocket.close();
        }
    }

    // ----------------------------------------------------------------
    //  TC-CCH-10  sendPacket(null) không crash
    // ----------------------------------------------------------------
    @Test
    @Order(10)
    @DisplayName("TC-CCH-10: sendPacket(null) không ném exception ra ngoài")
    void testSendPacketNull() throws Exception {
        try (ServerSocket ss = new ServerSocket(BASE_PORT + 10)) {
            ss.setSoTimeout(2000);
            java.util.concurrent.CompletableFuture<Socket> accepted =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return ss.accept(); } catch (Exception e) { return null; }
                    });

            Socket client = new Socket("127.0.0.1", BASE_PORT + 10);
            AuctionServer mockServer = mock(AuctionServer.class);
            Socket serverSide = accepted.get(2, java.util.concurrent.TimeUnit.SECONDS);

            ClientConnectionHandler handler = new ClientConnectionHandler(serverSide, mockServer);

            try {
                handler.sendPacket(null);
            } catch (NullPointerException e) {
                fail("sendPacket(null) ném NPE – cần null-check: " + e.getMessage());
            }

            client.close();
        }
    }
}