package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import com.uet.BiddingApplication.ServerClass.ClientConnectionHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nhạc trưởng Phát thanh: Quản lý mô hình Observer (Pub/Sub) để cập nhật dữ liệu Realtime.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class RealtimeBroadcastService {

    // Map lưu ID Phòng đấu giá và Tập hợp ID người dùng đang xem phòng đó.
    private ConcurrentHashMap<String, Set<String>> roomSubscribers;

    private static volatile RealtimeBroadcastService instance = null;

    private RealtimeBroadcastService(){
        this.roomSubscribers = new ConcurrentHashMap<>();
    }

    public static RealtimeBroadcastService getInstance(){
        if (instance == null){
            synchronized (RealtimeBroadcastService.class){
                if (instance == null){
                    instance = new RealtimeBroadcastService();
                }
            }
        }
        return instance;
    }

    /**
     * Thêm user vào danh sách theo dõi của một phòng đấu giá.
     */
    public void subscribe(String sessionId, String userId) {
        // 1. FAIL-FAST: Kiểm tra dữ liệu đầu vào ngay lập tức.
        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.err.println("[ERROR] Subscribe failed: sessionId is null or empty. UserId: " + userId);
            throw new IllegalArgumentException("SessionId cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("[ERROR] Subscribe failed: userId is null or empty. SessionId: " + sessionId);
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }

        try {
            // 2. THREAD-SAFETY: Sử dụng computeIfAbsent để đảm bảo Atomic Operation.
            Set<String> audience = roomSubscribers.computeIfAbsent(
                    sessionId,
                    k -> ConcurrentHashMap.newKeySet()
            );

            // 3. IDEMPOTENCY: Phương thức add() của Set trả về true nếu thêm mới, false nếu đã tồn tại.
            boolean isNewSubscriber = audience.add(userId);

            // 4. PRINT LOGGING: In ra màn hình console thay vì dùng thư viện Log
            if (isNewSubscriber) {
                System.out.println("[INFO] User [" + userId + "] successfully subscribed to room [" + sessionId + "]");
            } else {
                // Debug: Không spam log nếu user bấm F5 hoặc gửi nhầm request subscribe nhiều lần
                // (Bạn có thể comment dòng này lại nếu thấy console bị trôi quá nhanh)
                System.out.println("[DEBUG] User [" + userId + "] is already in room [" + sessionId + "]. Ignored duplicate subscription.");
            }

        } catch (Exception e) {
            // 5. ERROR HANDLING: Bắt và in ra lỗi chi tiết (Stack trace) để dễ sửa
            System.err.println("[ERROR] Unexpected error while subscribing user [" + userId + "] to room [" + sessionId + "]: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Internal error during subscription", e);
        }
    }

    /**
     * Xóa user khỏi tất cả các phòng (Dọn rác khi user thoát ra trang chủ).
     */
    public void unsubscribeFromAll(String userId) {
        if (userId == null) return;

        // Sử dụng entrySet và removeIf để duyệt và xóa an toàn trong môi trường đa luồng
        roomSubscribers.entrySet().removeIf(entry -> {
            Set<String> audience = entry.getValue();
            audience.remove(userId);
            // Xóa luôn entry này khỏi Map nếu Set đã trống
            return audience.isEmpty();
        });

        System.out.println("[INFO] Đã dọn dẹp toàn bộ phòng của User [" + userId + "]");
    }

    public void unsubscribe(String sessionId, String userId) {
        if (sessionId == null || userId == null) return;

        // Xóa an toàn và kiểm tra dọn rác bằng cơ chế của ConcurrentHashMap
        roomSubscribers.computeIfPresent(sessionId, (key, audience) -> {
            boolean removed = audience.remove(userId);
            if (removed) {
                System.out.println("[INFO] User [" + userId + "] left room [" + sessionId + "]");
            }

            // Trả về null sẽ báo cho ConcurrentHashMap tự động remove cái key (sessionId) này đi
            if (audience.isEmpty()) {
                System.out.println("[INFO] Room [" + sessionId + "] is empty and has been removed.");
                return null;
            }
            return audience; // Giữ nguyên phòng nếu vẫn còn người
        });
    }

    /**
     * Phát thanh một gói tin (Packet) đến toàn bộ user trong một phòng.
     */
    public void broadcast(String sessionId, ResponsePacket<?> packet) {
        // 1. Lấy danh sách khán giả đang theo dõi phòng này
        Set<String> audience = roomSubscribers.get(sessionId);

        // 2. Lập trình phòng thủ (Defensive Programming): Chặn lỗi NullPointer
        if (audience == null || audience.isEmpty()) {
            System.out.println("[DEBUG] Phòng [" + sessionId + "] không có khán giả để broadcast.");
            return;
        }

        // 3. Lấy trạm phát sóng (Server) ở ngoài vòng lặp để tránh gọi dư thừa
        AuctionServer server = AuctionServer.getInstance();

        // 4. Duyệt qua từng userId và phát dữ liệu
        for (String userId : audience) {
            // Nhờ Server tìm đúng đường ống kết nối mạng của user này
            ClientConnectionHandler clientHandler = server.getClientHandler(userId);

            // Kiểm tra chắc chắn user còn online thì mới gửi
            if (clientHandler != null) {
                clientHandler.sendPacket(packet);
            } else {
                // User có thể đã bị rớt mạng hoặc đóng app đột ngột
                System.out.println("[WARN] Broadcast thất bại: User [" + userId + "] không online.");
            }
        }
    }

    /**
     * (Nam) : Xóa toàn bộ phòng đấu giá khỏi bộ nhớ khi phiên kết thúc.
     * Không gọi Database ở đây để đảm bảo tốc độ Realtime.
     */
    public void closeRoom(String sessionId) {
        if (sessionId == null) return;

        Set<String> removedRoom = roomSubscribers.remove(sessionId);

        if (removedRoom != null) {
            System.out.println("[INFO] Đã đóng phòng phát thanh cho Session [" + sessionId + "]. Giải phóng " + removedRoom.size() + " users.");
        }
    }

    /**
     * Gửi tin nhắn riêng tư (Private Message) cho một User cụ thể.
     */
    public void sendPrivateMessage(String userId, ResponsePacket<?> packet) {
        try {
            // Lấy đường ống kết nối của đúng user đó từ Server
            var handler = AuctionServer.getInstance().getClientHandler(userId);
            if (handler != null) {
                handler.sendPacket(packet);
            } else {
                System.out.println("[DEBUG] Không thể gửi tin riêng: User [" + userId + "] có thể đã ngắt kết nối.");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Lỗi khi gửi tin nhắn riêng cho User [" + userId + "]: " + e.getMessage());
        }
    }
}