package com.uet.BiddingApplication.ServerClass;

import org.junit.jupiter.api.*;

import java.net.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho UDPDiscoveryServer
 *
 * Bao gồm:
 *  - Phản hồi đúng "I_AM_AUCTION_SERVER" khi nhận "WHERE_IS_AUCTION_SERVER"
 *  - Bỏ qua tin nhắn không đúng format
 *  - Dừng an toàn khi Thread bị interrupt()
 *  - Xử lý nhiều request liên tiếp
 */
@DisplayName("UDPDiscoveryServer Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UDPDiscoveryServerTest {

    private static final int UDP_PORT = 8888;
    private static final int CLIENT_PORT = 9001;
    private static final String DISCOVER_MSG  = "WHERE_IS_AUCTION_SERVER";
    private static final String RESPONSE_MSG  = "I_AM_AUCTION_SERVER";
    private static final int    TIMEOUT_MS    = 3000;

    private Thread serverThread;
    private UDPDiscoveryServer udpServer;

    @BeforeEach
    void startServer() throws Exception {
        // Đảm bảo port 8888 rảnh trước khi bắt đầu mỗi test
        udpServer = new UDPDiscoveryServer();
        serverThread = new Thread(udpServer);
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(200); // Chờ server bind xong
    }

    @AfterEach
    void stopServer() throws Exception {
        serverThread.interrupt();
        serverThread.join(2000);
    }

    // ----------------------------------------------------------------
    //  TC-UDP-01  Phản hồi đúng khi nhận đúng message
    // ----------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("TC-UDP-01: Gửi WHERE_IS_AUCTION_SERVER → nhận I_AM_AUCTION_SERVER")
    void testRespondsToDiscoverMessage() throws Exception {
        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT)) {
            clientSocket.setSoTimeout(TIMEOUT_MS);

            // Gửi discover broadcast
            byte[] sendData = DISCOVER_MSG.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length,
                    InetAddress.getByName("127.0.0.1"), UDP_PORT);
            clientSocket.send(sendPacket);

            // Đợi phản hồi
            byte[] recvBuf = new byte[1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            clientSocket.receive(recvPacket);

            String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
            assertEquals(RESPONSE_MSG, response,
                    "Server phải phản hồi đúng I_AM_AUCTION_SERVER");
        }
    }

    // ----------------------------------------------------------------
    //  TC-UDP-02  Bỏ qua tin nhắn không đúng format
    // ----------------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("TC-UDP-02: Gửi message sai format → không nhận phản hồi (timeout)")
    void testIgnoresWrongMessage() throws Exception {
        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT + 1)) {
            clientSocket.setSoTimeout(1000); // Timeout ngắn hơn

            byte[] sendData = "HELLO_SERVER".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length,
                    InetAddress.getByName("127.0.0.1"), UDP_PORT);
            clientSocket.send(sendPacket);

            // Server KHÔNG được phản hồi → phải timeout
            assertThrows(SocketTimeoutException.class, () -> {
                byte[] recvBuf = new byte[1024];
                DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
                clientSocket.receive(recvPacket);
            }, "Server không được phản hồi khi nhận message sai format");
        }
    }

    // ----------------------------------------------------------------
    //  TC-UDP-03  Phản hồi đúng địa chỉ IP của client gửi yêu cầu
    // ----------------------------------------------------------------
    @Test
    @Order(3)
    @DisplayName("TC-UDP-03: Phản hồi được gửi đúng về địa chỉ IP của client")
    void testResponseSentToCorrectAddress() throws Exception {
        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT + 2)) {
            clientSocket.setSoTimeout(TIMEOUT_MS);

            InetAddress serverAddr = InetAddress.getByName("127.0.0.1");
            byte[] sendData = DISCOVER_MSG.getBytes();
            clientSocket.send(new DatagramPacket(sendData, sendData.length, serverAddr, UDP_PORT));

            byte[] recvBuf = new byte[1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            clientSocket.receive(recvPacket);

            // Phản hồi phải đến từ 127.0.0.1
            assertEquals("127.0.0.1", recvPacket.getAddress().getHostAddress(),
                    "Phản hồi phải đến từ địa chỉ server (127.0.0.1)");
        }
    }

    // ----------------------------------------------------------------
    //  TC-UDP-04  Xử lý nhiều request liên tiếp
    // ----------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("TC-UDP-04: Server xử lý đúng 3 request liên tiếp")
    void testHandlesMultipleRequests() throws Exception {
        int successCount = 0;
        for (int i = 0; i < 3; i++) {
            try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT + 10 + i)) {
                clientSocket.setSoTimeout(TIMEOUT_MS);

                byte[] sendData = DISCOVER_MSG.getBytes();
                clientSocket.send(new DatagramPacket(
                        sendData, sendData.length,
                        InetAddress.getByName("127.0.0.1"), UDP_PORT));

                byte[] recvBuf = new byte[1024];
                DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
                clientSocket.receive(recvPacket);

                String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
                if (RESPONSE_MSG.equals(response)) successCount++;
            }
        }
        assertEquals(3, successCount, "Server phải phản hồi đúng cả 3 request");
    }

    // ----------------------------------------------------------------
    //  TC-UDP-05  Dừng an toàn khi Thread bị interrupt()
    // ----------------------------------------------------------------
    @Test
    @Order(5)
    @DisplayName("TC-UDP-05: Server dừng an toàn khi Thread bị interrupt()")
    void testStopsOnInterrupt() throws Exception {
        assertTrue(serverThread.isAlive(), "Server phải đang chạy trước interrupt");

        // 1. Cắm cờ ngắt luồng trước
        serverThread.interrupt();
        Thread.sleep(50); // Chờ một chút nhỏ để cờ được ghi nhận

        // 2. Gửi gói tin mồi bằng cách bind rõ ràng vào localhost để tránh bị GitHub Actions chặn
        try (DatagramSocket wakeUpSocket = new DatagramSocket()) {
            wakeUpSocket.setReuseAddress(true);
            byte[] dummyData = "WAKE_UP_SIGNAL".getBytes();
            // Sử dụng "localhost" thay vì "127.0.0.1" giúp một số runner tự động phân giải interface chuẩn hơn
            DatagramPacket wakeUpPacket = new DatagramPacket(
                    dummyData, dummyData.length,
                    InetAddress.getByName("localhost"), UDP_PORT);
            wakeUpSocket.send(wakeUpPacket);
        }

        // 3. Chờ tối đa 2 giây cho luồng phụ đóng hoàn toàn
        serverThread.join(2000);

        assertFalse(serverThread.isAlive(),
                "Luồng UDPDiscoveryServer phải dừng hẳn sau khi nhận tín hiệu ngắt");
    }
    // ----------------------------------------------------------------
    //  TC-UDP-06  Message rỗng – không crash, không phản hồi
    // ----------------------------------------------------------------
    @Test
    @Order(6)
    @DisplayName("TC-UDP-06: Gửi message rỗng → server không crash, không phản hồi")
    void testEmptyMessageNoResponse() throws Exception {
        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT + 20)) {
            clientSocket.setSoTimeout(1000);

            byte[] sendData = new byte[0];
            clientSocket.send(new DatagramPacket(
                    sendData, sendData.length,
                    InetAddress.getByName("127.0.0.1"), UDP_PORT));

            assertThrows(SocketTimeoutException.class, () -> {
                byte[] recvBuf = new byte[1024];
                clientSocket.receive(new DatagramPacket(recvBuf, recvBuf.length));
            }, "Server không được phản hồi khi nhận message rỗng");
        }

        // Server vẫn phải đang chạy sau khi nhận message rỗng
        Thread.sleep(200);
        assertTrue(serverThread.isAlive(),
                "Server phải tiếp tục chạy sau khi nhận message rỗng");
    }

    // ----------------------------------------------------------------
    //  TC-UDP-07  Phản hồi đúng sau message sai rồi đúng
    // ----------------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("TC-UDP-07: Gửi message sai rồi đúng → chỉ nhận phản hồi cho message đúng")
    void testCorrectMessageAfterWrongOne() throws Exception {
        // Gửi message sai (không chờ phản hồi)
        try (DatagramSocket wrongSocket = new DatagramSocket(CLIENT_PORT + 30)) {
            wrongSocket.setSoTimeout(500);
            byte[] wrongData = "GARBAGE".getBytes();
            wrongSocket.send(new DatagramPacket(
                    wrongData, wrongData.length,
                    InetAddress.getByName("127.0.0.1"), UDP_PORT));
        }

        Thread.sleep(100);

        // Gửi message đúng → phải nhận phản hồi
        try (DatagramSocket goodSocket = new DatagramSocket(CLIENT_PORT + 31)) {
            goodSocket.setSoTimeout(TIMEOUT_MS);
            byte[] goodData = DISCOVER_MSG.getBytes();
            goodSocket.send(new DatagramPacket(
                    goodData, goodData.length,
                    InetAddress.getByName("127.0.0.1"), UDP_PORT));

            byte[] recvBuf = new byte[1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            goodSocket.receive(recvPacket);

            String response = new String(recvPacket.getData(), 0, recvPacket.getLength());
            assertEquals(RESPONSE_MSG, response,
                    "Server vẫn phải phản hồi đúng sau khi nhận message sai trước đó");
        }
    }

    // ----------------------------------------------------------------
    //  TC-UDP-08  Concurrent: 3 client gửi đồng thời
    // ----------------------------------------------------------------
    @Test
    @Order(8)
    @DisplayName("TC-UDP-08: 3 client gửi discover đồng thời đều nhận được phản hồi")
    void testConcurrentDiscovery() throws Exception {
        int numClients = 3;
        boolean[] results = new boolean[numClients];
        Thread[] clients = new Thread[numClients];

        for (int i = 0; i < numClients; i++) {
            final int idx = i;
            clients[idx] = new Thread(() -> {
                try (DatagramSocket sock = new DatagramSocket(CLIENT_PORT + 40 + idx)) {
                    sock.setSoTimeout(TIMEOUT_MS);
                    byte[] data = DISCOVER_MSG.getBytes();
                    sock.send(new DatagramPacket(data, data.length,
                            InetAddress.getByName("127.0.0.1"), UDP_PORT));

                    byte[] buf = new byte[1024];
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    sock.receive(pkt);
                    String resp = new String(pkt.getData(), 0, pkt.getLength());
                    results[idx] = RESPONSE_MSG.equals(resp);
                } catch (Exception e) {
                    results[idx] = false;
                }
            });
        }

        for (Thread t : clients) t.start();
        for (Thread t : clients) t.join(5000);

        for (int i = 0; i < numClients; i++) {
            assertTrue(results[i], "Client " + i + " phải nhận được phản hồi đúng");
        }
    }
}