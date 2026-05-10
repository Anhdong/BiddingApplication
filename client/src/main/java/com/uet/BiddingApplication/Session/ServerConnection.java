package com.uet.BiddingApplication.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.Utils.GsonPacketParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;

public class ServerConnection {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServerConnection.class);

    // Singleton pattern
    private static volatile ServerConnection instance;

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

    public void connect(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            // Khởi tạo và chạy Luồng Lắng Nghe độc lập
            this.listenerThread = new ResponseListenerThread(in);
            this.threadHandle = new Thread(listenerThread);
            this.threadHandle.setDaemon(true); // Tự động chết khi app đóng
            this.threadHandle.start();

            log.info("Đã kết nối Server thành công và bật luồng lắng nghe!");
        } catch (Exception e) {
            log.error("Lỗi kết nối Server: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
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
        if (out != null) {
            try {
                String jsonStr = GsonPacketParser.serialize(request);
                out.println(jsonStr);
                // log.info("[Client -> Server] Đã gửi: " + jsonStr);
            } catch (Exception e) {
                log.error("Lỗi đóng gói dữ liệu: " + e.getMessage());
            }
        } else {
            log.error("Chưa kết nối Server, không thể gửi request!");
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
}