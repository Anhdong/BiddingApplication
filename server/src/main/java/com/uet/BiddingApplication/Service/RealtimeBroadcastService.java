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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RealtimeBroadcastService.class);

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
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.error("[ERROR] Subscribe failed: sessionId is null or empty. UserId: " + userId);
            throw new IllegalArgumentException("SessionId cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            log.error("[ERROR] Subscribe failed: userId is null or empty. SessionId: " + sessionId);
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }

        try {
            Set<String> audience = roomSubscribers.computeIfAbsent(
                    sessionId,
                    k -> ConcurrentHashMap.newKeySet()
            );

            boolean isNewSubscriber = audience.add(userId);

            if (isNewSubscriber) {
                log.info("[INFO] User [" + userId + "] successfully subscribed to room [" + sessionId + "]");
            } else {
                log.info("[DEBUG] User [" + userId + "] is already in room [" + sessionId + "]. Ignored duplicate subscription.");
            }

        } catch (Exception e) {
            log.error("[ERROR] Unexpected error while subscribing user [" + userId + "] to room [" + sessionId + "]: " + e.getMessage());
            log.error("Đã xảy ra lỗi Exception:", e);
            throw new RuntimeException("Internal error during subscription", e);
        }
    }

    /**
     * Xóa user khỏi tất cả các phòng (Dọn rác khi user thoát ra trang chủ).
     */
    public void unsubscribeFromAll(String userId) {
        if (userId == null) return;

        roomSubscribers.entrySet().removeIf(entry -> {
            Set<String> audience = entry.getValue();
            audience.remove(userId);
            return audience.isEmpty();
        });

        log.info("[INFO] Đã dọn dẹp toàn bộ phòng của User [" + userId + "]");
    }

    public void unsubscribe(String sessionId, String userId) {
        if (sessionId == null || userId == null) return;

        roomSubscribers.computeIfPresent(sessionId, (key, audience) -> {
            boolean removed = audience.remove(userId);
            if (removed) {
                log.info("[INFO] User [" + userId + "] left room [" + sessionId + "]");
            }

            if (audience.isEmpty()) {
                log.info("[INFO] Room [" + sessionId + "] is empty and has been removed.");
                return null;
            }
            return audience;
        });
    }

    /**
     * Phát thanh một gói tin (Packet) đến toàn bộ user trong một phòng.
     */
    public void broadcast(String sessionId, ResponsePacket<?> packet) {
        Set<String> audience = roomSubscribers.get(sessionId);

        if (audience == null || audience.isEmpty()) {
            log.info("[DEBUG] Phòng [" + sessionId + "] không có khán giả để broadcast.");
            return;
        }

        AuctionServer server = AuctionServer.getInstance();

        for (String userId : audience) {
            ClientConnectionHandler clientHandler = server.getClientHandler(userId);

            if (clientHandler != null) {
                clientHandler.sendPacket(packet);
            } else {
                log.info("[WARN] Broadcast thất bại: User [" + userId + "] không online.");
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
            log.info("[INFO] Đã đóng phòng phát thanh cho Session [" + sessionId + "]. Giải phóng " + removedRoom.size() + " users.");
        }
    }

    /**
     * Gửi tin nhắn riêng tư (Private Message) cho một User cụ thể.
     */
    public void sendPrivateMessage(String userId, ResponsePacket<?> packet) {
        try {
            var handler = AuctionServer.getInstance().getClientHandler(userId);
            if (handler != null) {
                handler.sendPacket(packet);
            } else {
                log.info("[DEBUG] Không thể gửi tin riêng: User [" + userId + "] có thể đã ngắt kết nối.");
            }
        } catch (Exception e) {
            log.error("[ERROR] Lỗi khi gửi tin nhắn riêng cho User [" + userId + "]: " + e.getMessage());
        }
    }
}