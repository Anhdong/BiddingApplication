package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;

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
    public void unsubscribeFromAll(String userId){
        // Duyệt qua tất cả các phòng hiện có
        for (Set<String> roomAudience : roomSubscribers.values()) {
            // Nếu Set chứa userId này thì xóa đi
            roomAudience.remove(userId);
        }
    }

    /**
     * Rút user khỏi một phòng cụ thể.
     */
    public void unsubscribe(String sessionId, String userId){
        if (sessionId == null || userId == null) return;

        Set<String> audience = roomSubscribers.get(sessionId);
        if (audience != null) {
            boolean removed = audience.remove(userId);
            if (removed) {
                System.out.println("[INFO] User [" + userId + "] left room [" + sessionId + "]");
            }

            // MẸO CHỐNG RÒ RỈ BỘ NHỚ (Memory Leak): Xóa luôn phòng nếu không còn ai xem
            if (audience.isEmpty()) {
                roomSubscribers.remove(sessionId);
                System.out.println("[INFO] Room [" + sessionId + "] is empty and has been removed from memory.");
            }
        }
    }

    /**
     * Phát thanh một gói tin (Packet) đến toàn bộ user trong một phòng.
     */
    public void broadcast(String sessionId, ResponsePacket<?> packet){
        // TODO 1 (Input): Nhận sessionId và gói dữ liệu (packet) cần phát.
        // TODO 2 (Dependencies): Lấy Set các userId đang theo dõi phòng này.
        // TODO 3 (Processing): Dùng vòng lặp gọi AuctionServer.getInstance().getClientHandler(userId) để lấy luồng mạng.
        // TODO 4 (Output): Gọi handler.sendPacket(packet) để đẩy dữ liệu xuống.
//        Set<String> audience = roomSubscribers.get(sessionId);
//        ClientConnectionHandler handler = AuctionServer.getInstance();
//        for (String userId : audience){
//            handler.getClientHandler(userId);
//        }
//        handler.sendPacket(packet);
    }
    /**
     * (Nam) : Bổ sung thêm 1 phương thức closeRoom() nữa, tác dụng là xóa phiên đấu giá khỏi map
     * khi phiên kết thúc
     */
    public void closeRoom(String sessionId){
        AuctionSession session = AuctionSessionDAO.getInstance().getSessionById(sessionId);

        if (session.getStatus() == SessionStatus.RUNNING || session.getStatus() == SessionStatus.OPEN){
            throw new BusinessException("Phiên đang chạy không thể xóa phiên.");
        }

        roomSubscribers.remove(sessionId);
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