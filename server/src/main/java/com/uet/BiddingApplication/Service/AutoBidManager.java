package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.InMemoryBidServiceImpl;
import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.AutoBidSetting;
// Giả định import DAO bạn vừa tạo
import com.uet.BiddingApplication.DAO.Impl.AutoBidSettingDAO;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lớp quản lý đấu giá tự động (Auto-Bidding) cho người dùng.
 * Áp dụng mẫu thiết kế Singleton (Double-checked locking).
 */
public class AutoBidManager {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AutoBidManager.class);

    private ConcurrentHashMap<String, ConcurrentLinkedQueue<AutoBidSetting>> autoBidQueues;

    private static volatile AutoBidManager instance = null;

    private AutoBidManager() {
        this.autoBidQueues = new ConcurrentHashMap<>();
    }

    public static AutoBidManager getInstance() {
        if (instance == null) {
            synchronized (AutoBidManager.class) {
                if (instance == null) {
                    instance = new AutoBidManager();
                }
            }
        }
        return instance;
    }

    /**
     * Đăng ký cài đặt trả giá tự động cho một phiên.
     */
    public void registerAutoBid(AutoBidSetting setting) {
        if (setting == null || setting.getSessionId() == null || setting.getBidderId() == null) {
            return;
        }

        // [LOGIC DATABASE 1]: Lưu xuống DB trước.
        // Sử dụng hàm DAO (Upsert hoặc Delete rồi Insert) để đảm bảo ràng buộc UNIQUE dưới Database.
        boolean dbSuccess = AutoBidSettingDAO.getInstance().upsertAutoBid(setting);
        if (!dbSuccess) {
            log.error("[LỖI] Không thể lưu Auto-bid xuống Database cho User [" + setting.getBidderId() + "]. Hủy thao tác trên RAM.");
            return;
        }

        ConcurrentLinkedQueue<AutoBidSetting> queue = autoBidQueues.computeIfAbsent(
                setting.getSessionId(),
                k -> new ConcurrentLinkedQueue<>()
        );

        // Chống Spam/Trùng lặp trên RAM
        queue.removeIf(existing -> existing.getBidderId().equals(setting.getBidderId()));

        queue.add(setting);
        log.info("[INFO] Đăng ký Auto-bid thành công cho User [" + setting.getBidderId() + "] tại phiên [" + setting.getSessionId() + "]");
    }

    /**
     * Hủy bỏ cài đặt trả giá tự động theo yêu cầu chủ động của người dùng.
     */
    public void cancelAutoBid(String sessionId, String bidderId) {
        if (sessionId == null || bidderId == null) return;

        ConcurrentLinkedQueue<AutoBidSetting> queue = autoBidQueues.get(sessionId);
        if (queue != null) {
            // Cập nhật trên RAM
            boolean removed = queue.removeIf(setting -> setting.getBidderId().equals(bidderId));

            if (removed) {
                // [LOGIC DATABASE 2]: Xóa khỏi DB (Bất đồng bộ để phản hồi Client nhanh nhất)
                CompletableFuture.runAsync(() -> {
                    AutoBidSettingDAO.getInstance().deleteAutoBid(bidderId,sessionId);
                });
                log.info("[INFO] User [" + bidderId + "] đã hủy Auto-bid tại phiên [" + sessionId + "]");
            }

            // Dọn phòng trống trên RAM
            if (queue.isEmpty()) {
                autoBidQueues.remove(sessionId);
            }
        }
    }

    /**
     * Kích hoạt kiểm tra và đặt giá tự động khi có một mức giá mới được duyệt.
     */
    public void triggerAutoBid(String sessionId, BigDecimal currentPrice, String highestBidderId) {
        ConcurrentLinkedQueue<AutoBidSetting> queue = autoBidQueues.get(sessionId);

        if (queue == null || queue.isEmpty()) {
            return;
        }

        AuctionSession session = SearchCacheManager.getInstance().getSession(sessionId);
        if (session == null) return;

        Iterator<AutoBidSetting> iterator = queue.iterator();

        while (iterator.hasNext()) {
            AutoBidSetting setting = iterator.next();

            // --- ĐIỀU KIỆN 1: Đang là người dẫn đầu ---
            if (setting.getBidderId().equals(highestBidderId)) {
                continue;
            }

            // --- ĐIỀU KIỆN 2: Tính toán bước giá hợp lệ ---
            BigDecimal validIncrement = setting.getIncrement().max(session.getBidStep());
            BigDecimal nextBidPrice = currentPrice.add(validIncrement);

            // --- ĐIỀU KIỆN 3: Chạm ngưỡng Max Bid (Hết tiền) ---
            if (setting.getMaxBid().compareTo(nextBidPrice) < 0) {
                // a) Xóa vĩnh viễn user này khỏi hàng đợi Auto-bid RAM
                iterator.remove();

                // b) [LOGIC DATABASE 3]: Xóa vĩnh viễn khỏi DB (Bắt buộc dùng luồng riêng để không kẹt luồng AutoBid)
                CompletableFuture.runAsync(() -> {
                    AutoBidSettingDAO.getInstance().deleteAutoBid(sessionId, setting.getBidderId());
                });

                // c) Đóng gói Packet thông báo
                ResponsePacket<Void> cancelPacket = new ResponsePacket<>(
                        ActionType.CANCEL_AUTO_BID,
                        400,
                        "Hệ thống đã ngừng Auto-Bid do giới hạn giá tối đa của bạn không đủ để theo vòng mới.",
                        null
                );

                // d) Gửi thông báo riêng rẽ
                RealtimeBroadcastService.getInstance().sendPrivateMessage(setting.getBidderId(), cancelPacket);
                log.info("[INFO] Đã gỡ Auto-Bid của User [" + setting.getBidderId() + "] do chạm ngưỡng maxBid.");

                continue;
            }

            // --- ĐIỀU KIỆN 4: Đủ điều kiện đấu giá ---
            BidRequestDTO autoRequest = new BidRequestDTO();
            autoRequest.setSessionId(sessionId);
            autoRequest.setBidAmount(nextBidPrice);

            log.info("[INFO] Kích hoạt Auto-Bid cho User [" + setting.getBidderId() + "], giá đặt tự động: " + nextBidPrice);

            InMemoryBidServiceImpl.getInstance().enqueueBid(autoRequest, setting.getBidderId());

            break;
        }
    }

    /**
     * Dọn dẹp toàn bộ AutoBid trên RAM khi phiên kết thúc.
     * (Lệnh xóa DB toàn bộ theo SessionId sẽ được gọi ở InMemoryBidServiceImpl.handleAuctionEnd)
     */
    public void clearSessionQueue(String sessionId) {
        if (sessionId != null) {
            autoBidQueues.remove(sessionId);
            AutoBidSettingDAO.getInstance().deleteAllBySessionId(sessionId);
            log.info("[INFO] Đã dọn dẹp hàng đợi Auto-bid cho phiên [" + sessionId + "]");
        }
    }
    /**
     * Dọn dẹp toàn bộ Auto-bid trên RAM (và DB) khi một User bị khóa/ban tài khoản.
     * Đảm bảo User bị ban không thể tiếp tục "bóng ma" trả giá ở bất kỳ phiên nào.
     * * @param bannedUserId ID của người dùng vừa bị khóa/xóa.
     */
    public void removeAutoBidsForBannedUser(String bannedUserId) {
        if (bannedUserId == null) return;
        AutoBidSettingDAO.getInstance().deleteAllByBidderId(bannedUserId);

        int removedCount = 0;

        // Duyệt qua tất cả các hàng đợi Auto-bid của tất cả các phiên đang chạy
        // Dùng entrySet() thay vì keySet() để duyệt nhanh hơn (O(n) thay vì O(n log n))
        for (java.util.Map.Entry<String, ConcurrentLinkedQueue<AutoBidSetting>> entry : autoBidQueues.entrySet()) {
            String sessionId = entry.getKey();
            ConcurrentLinkedQueue<AutoBidSetting> queue = entry.getValue();

            // Lọc và xóa ngay lập tức user bị ban khỏi hàng đợi của phiên này
            boolean removed = queue.removeIf(setting -> setting.getBidderId().equals(bannedUserId));

            if (removed) {
                removedCount++;
                // Tối ưu RAM: Xóa luôn cái key của phòng nếu phòng đó không còn ai dùng Auto-bid nữa
                if (queue.isEmpty()) {
                    autoBidQueues.remove(sessionId);
                }
            }
        }

        if (removedCount > 0) {
            log.info("[INFO] BAN USER: Đã dọn dẹp thành công " + removedCount + " cài đặt Auto-bid của User [" + bannedUserId + "].");
        }
    }
}