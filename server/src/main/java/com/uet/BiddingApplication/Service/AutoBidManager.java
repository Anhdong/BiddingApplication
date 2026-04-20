package com.uet.BiddingApplication.Service;
/**
 * @TODO [Dành cho Thành viên 3 - Service]
 * Nâng cấp trải nghiệm người dùng (UX) và chống kẹt Queue cho Auto-Bid.
 *
 * YÊU CẦU CẬP NHẬT:
 * 1. Đổi kiểu dữ liệu của Queue từ PriorityQueue sang ConcurrentLinkedQueue (FIFO)
 *    để đảm bảo tính công bằng theo thời gian đăng ký (ai vào trước xử lý trước).
 *
 * 2. Cập nhật logic trong vòng lặp của hàm triggerAutoBid():
 *    - Khi duyệt Iterator, nếu phát hiện user có maxBid < currentPrice + increment:
 *      a) Gọi iterator.remove() để xóa vĩnh viễn cài đặt này khỏi Queue.
 *      b) THÔNG BÁO CHO USER: Đóng gói AutoBidCancelResponseDTO (chứa sessionId, bidderId).
 *      c) Gọi RealtimeBroadcastService.sendPrivateMessage(bidderId, packet)< cần tạo thêm hàm này >để báo cho
 *         máy khách tự động tắt nút Auto-bid trên giao diện.
 *      d) Dùng 'continue' để xét tiếp người phía sau trong Queue.
 *
 *    - Nếu user đủ tiền NHƯNG đang là người dẫn đầu (highestBidderId):
 *      Chỉ dùng 'continue' để bỏ qua (không xóa khỏi Queue, chống tự cắn đuôi).
 *
 *    - Nếu tìm thấy user hợp lệ đầu tiên: Đẩy BidRequestDTO vào InMemoryBidServiceImpl
 *      và 'break' (KẾT THÚC HÀM NGAY LẬP TỨC) để nhường luồng cho Tầng Core xử lý.
 */
import com.uet.BiddingApplication.CoreService.InMemoryBidServiceImpl;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;
import com.uet.BiddingApplication.DTO.Response.ResponsePacket;
import com.uet.BiddingApplication.Model.AutoBidSetting;
// Các import giả định tùy theo cấu trúc thư mục của nhóm bạn:
// import com.uet.BiddingApplication.Network.AuctionServer;
// import com.uet.BiddingApplication.Network.ClientConnectionHandler;
// import com.uet.BiddingApplication.Enum.ActionType;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lớp quản lý đấu giá tự động (Auto-Bidding) cho người dùng.
 * Áp dụng mẫu thiết kế Singleton (Double-checked locking).
 */
public class AutoBidManager {

    // Sử dụng ConcurrentLinkedQueue (FIFO) thay cho PriorityQueue để đảm bảo công bằng: ai vào trước, xử lý trước.
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

        ConcurrentLinkedQueue<AutoBidSetting> queue = autoBidQueues.computeIfAbsent(
                setting.getSessionId(),
                k -> new ConcurrentLinkedQueue<>()
        );

        // Chống Spam/Trùng lặp: Xóa cài đặt cũ của người này (nếu có) trước khi nhét cài đặt mới vào.
        queue.removeIf(existing -> existing.getBidderId().equals(setting.getBidderId()));

        queue.add(setting);
        System.out.println("[INFO] Đăng ký Auto-bid thành công cho User [" + setting.getBidderId() + "] tại phiên [" + setting.getSessionId() + "]");
    }

    /**
     * Hủy bỏ cài đặt trả giá tự động theo yêu cầu chủ động của người dùng.
     */
    public void cancelAutoBid(String sessionId, String bidderId) {
        if (sessionId == null || bidderId == null) return;

        ConcurrentLinkedQueue<AutoBidSetting> queue = autoBidQueues.get(sessionId);
        if (queue != null) {
            // Xóa nhanh chóng và an toàn trong đa luồng (Java 8+)
            boolean removed = queue.removeIf(setting -> setting.getBidderId().equals(bidderId));

            if (removed) {
                System.out.println("[INFO] User [" + bidderId + "] đã hủy Auto-bid tại phiên [" + sessionId + "]");
            }

            // Mẹo tối ưu RAM: Nếu phòng đã trống, dọn luôn phòng đó khỏi HashMap
            if (queue.isEmpty()) {
                autoBidQueues.remove(sessionId);
            }
        }
    }

    /**
     * Kích hoạt kiểm tra và đặt giá tự động khi có một mức giá mới được duyệt.
     * @param sessionId ID của phiên.
     * @param currentPrice Giá hiện tại vừa được ai đó đặt thành công.
     * @param highestBidderId ID của người vừa đặt giá thành công (để tránh tự đôn giá).
     */
    public void triggerAutoBid(String sessionId, BigDecimal currentPrice, String highestBidderId) {
        ConcurrentLinkedQueue<AutoBidSetting> queue = autoBidQueues.get(sessionId);

        if (queue == null || queue.isEmpty()) {
            return; // Phòng không có ai đăng ký auto-bid
        }

        // Sử dụng Iterator để có thể an toàn xóa (remove) phần tử trong quá trình duyệt
        Iterator<AutoBidSetting> iterator = queue.iterator();

        while (iterator.hasNext()) {
            AutoBidSetting setting = iterator.next();

            // Tính toán mức giá tiếp theo mà hệ thống định đặt thay cho người này
            BigDecimal nextBidPrice = currentPrice.add(setting.getIncrement());

            // --- ĐIỀU KIỆN 1: Chạm ngưỡng Max Bid (Hết tiền) ---
            if (setting.getMaxBid().compareTo(nextBidPrice) < 0) {
                // a) Xóa vĩnh viễn user này khỏi hàng đợi Auto-bid
                iterator.remove();

                // b) Đóng gói Packet thông báo
                // (Thay thế 'null' đầu tiên bằng ActionType.CANCEL_AUTO_BID của hệ thống bạn)
                ResponsePacket<Void> cancelPacket = new ResponsePacket<>(
                        null,
                        400,
                        "Hệ thống đã ngừng Auto-Bid do số tiền giới hạn (Max Bid) không đủ để trả giá tiếp.",
                        null
                );

                // c) Gọi đúng phương thức Tech Lead yêu cầu
                RealtimeBroadcastService.getInstance().sendPrivateMessage(setting.getBidderId(), cancelPacket);

                System.out.println("[INFO] Đã gỡ Auto-Bid của User [" + setting.getBidderId() + "] do chạm ngưỡng maxBid.");

                // d) Dùng 'continue' để xét tiếp người phía sau trong Queue
                continue;
            }

            // --- ĐIỀU KIỆN 2: Đang là người dẫn đầu (Chống tự đôn giá) ---
            if (setting.getBidderId().equals(highestBidderId)) {
                // Không làm gì cả, giữ nguyên vị trí trong Queue và đi tiếp
                continue;
            }

            // --- ĐIỀU KIỆN 3: Đủ điều kiện đấu giá ---
            // Đóng gói request giả lập như Client vừa bấm nút
            BidRequestDTO autoRequest = new BidRequestDTO();
            autoRequest.setSessionId(sessionId);
            autoRequest.setBidAmount(nextBidPrice);

            System.out.println("[INFO] Kích hoạt Auto-Bid cho User [" + setting.getBidderId() + "], giá đặt tự động: " + nextBidPrice);

            // Đẩy lệnh trả giá vào lưới xử lý lõi (Core Tier)
            InMemoryBidServiceImpl.getInstance().enqueueBid(autoRequest, setting.getBidderId());

            // Lệnh BREAK CỰC KỲ QUAN TRỌNG:
            // Mỗi lần có biến động giá, ta chỉ đẩy yêu cầu của 1 NGƯỜI DUY NHẤT (xếp hàng đầu tiên) vào Core.
            // Nếu giá đó được Core duyệt thành công, Core sẽ gọi lại hàm triggerAutoBid() này
            // và vòng lặp sẽ tự tìm đến đối thủ tiếp theo. Tránh việc spam 10 request cùng lúc.
            break;
        }
    }
}