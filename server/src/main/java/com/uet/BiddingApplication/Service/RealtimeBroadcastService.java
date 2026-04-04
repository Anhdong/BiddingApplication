package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nhạc trưởng Phát thanh: Quản lý mô hình Observer (Pub/Sub) để cập nhật dữ liệu Realtime.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class RealtimeBroadcastService {

    // Map lưu ID Phòng đấu giá và Tập hợp ID người dùng đang xem phòng đó.
    private ConcurrentHashMap<String, Set<String>> subscribers;

    private static volatile RealtimeBroadcastService instance = null;

    private RealtimeBroadcastService(){
        this.subscribers = new ConcurrentHashMap<>();
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
    public void subscribe(String sessionId, String userId){
        // TODO 1 (Input): Nhận sessionId và userId.
        // TODO 2 (Processing): Lấy Set các userId đang trong phòng này (Tạo mới ConcurrentHashMap.newKeySet() nếu chưa có).
        // TODO 3 (Side-effect): Thêm userId vào Set.
    }

    /**
     * Xóa user khỏi tất cả các phòng (Dọn rác khi user thoát ra trang chủ).
     */
    public void unsubscribeFromAll(String userId){
        // TODO 1 (Input): Nhận userId cần dọn dẹp.
        // TODO 2 (Processing): Duyệt qua toàn bộ tập hợp Set trong Map subscribers.
        // TODO 3 (Side-effect): Xóa userId này khỏi bất kỳ Set nào chứa nó.
    }

    /**
     * Rút user khỏi một phòng cụ thể.
     */
    public void unsubscribe(String sessionId, String userId){
        // TODO 1 (Input): Nhận sessionId và userId.
        // TODO 2 (Side-effect): Rút userId khỏi Set của phòng tương ứng.
    }

    /**
     * Phát thanh một gói tin (Packet) đến toàn bộ user trong một phòng.
     */
    public void broadcast(String sessionId, ResponsePacket<?> packet){
        // TODO 1 (Input): Nhận sessionId và gói dữ liệu (packet) cần phát.
        // TODO 2 (Dependencies): Lấy Set các userId đang theo dõi phòng này.
        // TODO 3 (Processing): Dùng vòng lặp gọi AuctionServer.getInstance().getClientHandler(userId) để lấy luồng mạng.
        // TODO 4 (Output): Gọi handler.sendPacket(packet) để đẩy dữ liệu xuống.
    }
}