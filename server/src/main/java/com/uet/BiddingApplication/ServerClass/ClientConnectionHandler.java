package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Utils.GsonPacketParser;
import com.uet.BiddingApplication.Service.RealtimeBroadcastService; // Import service quản lý phòng

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handler quản lý luồng kết nối I/O cho một Client cụ thể.
 * Được thiết kế Thread-safe để chạy mượt mà trong CachedThreadPool.
 */
public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;

    private BufferedReader in;
    private PrintWriter out;

    private String userId;

    // TỐI ƯU 1: Object khóa riêng biệt dùng cho việc gửi dữ liệu, tránh block toàn bộ Handler
    private final Object sendLock = new Object();

    public ClientConnectionHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
        try {
            // Ép kiểu UTF-8 chuẩn xác để không bị lỗi font tiếng Việt khi chạy cross-platform
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("[Handler] Lỗi khởi tạo luồng I/O: " + e.getMessage());
            forceClose("Lỗi khởi tạo luồng I/O");
        }
    }

    @Override
    public void run() {
        try {
            String jsonLine;
            // Lặp vô hạn để nhận lệnh từ Client cho đến khi mất kết nối
            while ((jsonLine = in.readLine()) != null) {
                System.out.println("[Server nhận từ " + (userId != null ? userId : "Guest") + "] " + jsonLine);

                // Dịch chuỗi JSON thành DTO
                RequestPacket<?> requestPacket = GsonPacketParser.deserializeRequest(jsonLine);

                // Chuyển hướng bản tin cho Router phân tải
                if (requestPacket != null) {
                    RequestRouter.getInstance().route(requestPacket, this);
                }
            }
        } catch (IOException e) {
            System.err.println("[Handler] Kết nối của " + (userId != null ? userId : "Guest") + " bị ngắt: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Handler] Lỗi hệ thống khi xử lý kết nối: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // =========================================================================
            // TỐI ƯU 2: KHỐI DỌN DẸP BỘ NHỚ (CLEANUP) - CHỐNG MEMORY LEAK & GHOST PACKET
            // =========================================================================

            // 1. Đóng kết nối mạng vật lý đầu tiên
            forceClose("Luồng đọc dữ liệu kết thúc (Client ngắt kết nối hoặc lỗi mạng)");

            if (userId != null) {
                // 2. Dọn dẹp phòng Realtime: Đảm bảo Server không gửi tin vào kết nối chết
                try {
                    // Gọi hàm xóa theo đúng góp ý của bạn để chống lỗi phòng đấu giá
                    RealtimeBroadcastService.getInstance().unsubscribeFromAll(userId);
                } catch (Exception e) {
                    System.err.println("[Handler] Lỗi dọn dẹp Realtime cho user " + userId + ": " + e.getMessage());
                }

                // 3. Xóa Client khỏi danh sách quản lý trực tuyến của Server
                try {
                    if (server != null) {
                        server.unregisterClient(userId);
                    }
                } catch (Exception e) {
                    System.err.println("[Handler] Lỗi unregister client " + userId + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gửi ResponsePacket về Client một cách đồng bộ (Synchronized)
     */
    public void sendPacket(ResponsePacket<?> packet) {
        if (out == null || socket.isClosed()) return;
        try {
            // Đẩy quá trình dịch JSON tốn CPU ra ngoài vùng khóa
            String jsonStr = GsonPacketParser.serialize(packet);

            // Chỉ khóa đúng lúc thao tác vào luồng I/O mạng
            synchronized (sendLock) {
                out.println(jsonStr); // TỐI ƯU 3: Bắt buộc dùng println thay vì print để readLine() phía nhận đọc được
                out.flush(); // Xả đệm để gửi đi ngay lập tức
            }
        } catch (Exception e) {
            System.err.println("[Handler] Lỗi gửi gói tin: " + e.getMessage());
        }
    }

    /**
     * Đóng kết nối bắt buộc và giải phóng tài nguyên I/O
     */
    public void forceClose(String reason) {
        System.out.println("[Handler] Đóng kết nối Socket của " + (userId != null ? userId : "Guest") + " - Lý do: " + reason);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Handler] Lỗi khi đóng socket: " + e.getMessage());
        }
    }

    public void closeConnection() {
        forceClose("Server chủ động yêu cầu đóng kết nối (Không rõ lý do cụ thể)");
    }

    public void closeConnection(String reason) {
        forceClose(reason);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        if (userId != null && server != null) {
            // Khai báo với Server rằng Client này đã đăng nhập thành công
            server.registerClient(userId, this);
        }
    }
}