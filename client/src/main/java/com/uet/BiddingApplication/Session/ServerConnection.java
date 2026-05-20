package com.uet.BiddingApplication.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.Util.NotificationUtil;
import com.uet.BiddingApplication.Utils.GsonPacketParser;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;

public class ServerConnection {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServerConnection.class);

    // Singleton pattern
    private static volatile ServerConnection instance;
    private volatile boolean connected = false; // Cờ kiểm soát trạng thái
    private volatile boolean isReconnecting = false; // Cờ chặn việc chạy nhiều luồng Reconnect cùng lúc

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private ResponseListenerThread listenerThread;
    private Thread threadHandle;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            synchronized (ServerConnection.class) {
                if (instance == null) {
                    instance = new ServerConnection();
                }
            }
        }
        return instance;
    }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean connect(String host, int port) {
        try {
            // DỌN DẸP XÁC CŨ TRƯỚC KHI NỐI LẠI
            if (socket != null) { try { socket.close(); } catch (Exception e){} }
            if (in != null) { try { in.close(); } catch (Exception e){} }
            if (out != null) { try { out.close(); } catch (Exception e){} }

            // TẠO MỚi
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            // Khởi tạo và chạy Luồng Lắng Nghe độc lập
            this.listenerThread = new ResponseListenerThread(in);
            this.threadHandle = new Thread(listenerThread);
            this.threadHandle.setDaemon(true); // Tự động chết khi app đóng
            this.threadHandle.start();

            log.info("Đã kết nối Server thành công và bật luồng lắng nghe!");
            this.connected = true;
            return true;
        } catch (Exception e) {
            log.error("Lỗi kết nối Server: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            this.connected = false;
            // 1. Ra lệnh cho luồng nghe dừng lại
            if (listenerThread != null) {
                listenerThread.stopListening();
            }
            // 2. Đóng ống nước và Socket
            if (socket != null && !socket.isClosed()) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();

            // 3. Ép luồng dừng hẳn (nếu đang bị block ở readLine)
            if (threadHandle != null) {
                threadHandle.interrupt();
            }

            log.info("Đã ngắt kết nối an toàn.");
        } catch (Exception e) {
            log.error("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    // Hàm thay thế cho out.print() rải rác ở khắp nơi
    public void sendRequest(RequestPacket<?> request) {
        if (!connected || out == null) {
            log.info("[Client] Đang mất mạng, hủy bỏ thao tác gửi gói tin: " + request.getAction());

            // Nhờ JavaFX Thread báo lỗi lên màn hình để user không bấm mù quáng nữa
            javafx.application.Platform.runLater(() -> {
                // Hiển thị Toast hoặc Alert tùy bạn
                NotificationUtil.showAlert(Alert.AlertType.ERROR,"Mất kết nối", "Hệ thống đang gián đoạn, vui lòng chờ trong giây lát...");
            });
            return;
        }
        try {
            String jsonStr = GsonPacketParser.serialize(request);
            out.println(jsonStr);
            // log.info("[Client -> Server] Đã gửi: " + jsonStr);
        } catch (Exception e) {
            log.error("Lỗi đóng gói dữ liệu: " + e.getMessage());
        }
    }
    /**
     * Dùng UDP Broadcast để tìm IP của Server trong mạng LAN
     * @return Địa chỉ IP của Server, hoặc null nếu không tìm thấy
     */
    public String discoverServerOnLAN() {
        try (DatagramSocket c = new DatagramSocket()) {
            c.setBroadcast(true); // Cho phép hét toàn mạng
            c.setSoTimeout(3000); // Chỉ đợi 3 giây, quá hạn là bỏ cuộc

            byte[] sendData = "WHERE_IS_AUCTION_SERVER".getBytes();

            // 255.255.255.255 là địa chỉ vạn năng để gửi cho TẤT CẢ máy trong Wifi
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);

            c.send(sendPacket);
            log.info("[Client] Đang rò tìm Server Đấu giá trên mạng LAN...");

            // Đợi Server trả lời
            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            if (response.equals("I_AM_AUCTION_SERVER")) {
                String serverIp = receivePacket.getAddress().getHostAddress();
                log.info("[Client] ĐÃ TÌM THẤY SERVER TẠI: " + serverIp);
                return serverIp;
            }

        } catch (SocketTimeoutException e) {
            log.info("[Client] Quá 3 giây không thấy Server nào lên tiếng.");
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi Exception:", e);
        }
        return null;
    }
    /**
     * Kích hoạt luồng tự động tìm và kết nối lại
     */
    public void startAutoReconnect() {
        if (isReconnecting) return; // Nếu đang tìm rồi thì không tạo thêm luồng nữa
        isReconnecting = true;

        log.error("[Watchdog] Phát hiện rớt mạng. Bắt đầu tiến trình Auto-Reconnect...");

        Platform.runLater(()->{
            NotificationUtil.showAlert(Alert.AlertType.ERROR,"Lỗi Kết Nối","Mất kết nối đến server, tiến hành kết nối lại");
        });

        Thread reconnectThread = new Thread(() -> {
            while (isReconnecting) {
                try {
                    // 1. Dùng hàm UDP quét lại mạng
                    String serverIp = discoverServerOnLAN();

                    // 2. Nếu quét không thấy, fallback về localhost
                    if (serverIp == null) {
                        serverIp = "127.0.0.1";
                    }

                    // 3. Thử kết nối TCP (Lưu ý: Bạn phải sửa hàm connect một chút để nó return true/false)
                    boolean success = connect(serverIp, 8080);

                    if (success) {
                        isReconnecting = false; // Dừng vòng lặp
                        log.info("[Watchdog] Kết nối lại THÀNH CÔNG tại IP: " + serverIp);

                        Platform.runLater(()->{
                            NotificationUtil.showAlert(Alert.AlertType.INFORMATION,"Kết Nối Thành Công","Đã khôi phục kết nối mạng");
                        });
                        break;
                    }

                    // Thất bại thì ngủ 3 giây rồi thử lại, tránh ăn 100% CPU
                    Thread.sleep(3000);

                } catch (Exception e) {
                    log.error("[Watchdog] Thử lại thất bại, đang chờ nhịp tiếp theo...");
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                }
            }
        });

        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }
}