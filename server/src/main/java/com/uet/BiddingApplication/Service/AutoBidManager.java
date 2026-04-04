package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.Model.AutoBidSetting;

import java.math.BigDecimal;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý đấu giá tự động (Auto-Bidding) cho người dùng.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class AutoBidManager {

    // Quản lý danh sách đặt tự động (RAM Cache).
    private ConcurrentHashMap<String, PriorityQueue<AutoBidSetting>> autoBidQueues;

    private static volatile AutoBidManager instance = null;

    private AutoBidManager(){
        this.autoBidQueues = new ConcurrentHashMap<>();
    }

    public static AutoBidManager getInstance(){
        if (instance == null){
            synchronized (AutoBidManager.class){
                if (instance == null){
                    instance = new AutoBidManager();
                }
            }
        }
        return instance;
    }

    /**
     * Đăng ký cài đặt trả giá tự động cho một phiên.
     */
    public void registerAutoBid(AutoBidSetting setting){
        // TODO 1 (Input): Nhận cấu hình AutoBid (gồm maxBid, increment, sessionId).
        // TODO 2 (Processing): Lấy PriorityQueue của sessionId từ autoBidQueues (Tạo mới nếu chưa có, nhớ setup Comparator).
        // TODO 3 (Side-effect): Thêm cấu hình setting vào Queue.
    }

    /**
     * Hủy bỏ cài đặt trả giá tự động.
     */
    public void cancelAutoBid(String sessionId, String bidderId){
        // TODO 1 (Input): Nhận sessionId và bidderId.
        // TODO 2 (Processing): Lấy PriorityQueue tương ứng của phiên.
        // TODO 3 (Side-effect): Xóa phần tử thuộc về bidderId khỏi Queue.
    }

    /**
     * Kích hoạt kiểm tra và đặt giá tự động khi có một mức giá mới được duyệt.
     */
    public void triggerAutoBid(String sessionId, BigDecimal currentPrice){
        // TODO 1 (Input): Nhận sessionId và mức giá hiện tại (currentPrice).
        // TODO 2 (Processing): Lấy người đứng đầu PriorityQueue (peek).
        // TODO 3 (Processing): Kiểm tra điều kiện: (currentPrice + increment) <= maxBid.
        // TODO 4 (Output/Side-effect): Nếu thỏa mãn, đóng gói thành BidRequestDTO và gọi InMemoryBidServiceImpl.getInstance().enqueueBid(...) để đặt giá.
    }
}