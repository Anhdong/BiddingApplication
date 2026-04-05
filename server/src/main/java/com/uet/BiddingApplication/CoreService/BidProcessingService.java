package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;

/**
 * Interface định nghĩa hợp đồng (contract) cho hệ thống lõi xử lý đặt giá đa luồng.
 *
 * Kiến trúc áp dụng: Hybrid Concurrency (Producer-Consumer + Consistent Hashing).
 * Mục tiêu: Đảm bảo tính tuần tự tuyệt đối (Lock-free) cho các lượt trả giá của cùng một phiên,
 * ngăn chặn hoàn toàn lỗi Race Condition (Lost Update) mà không làm nghẽn đường truyền mạng.
 */
public interface BidProcessingService {

    /**
     * TÁC VỤ PRODUCER: Dành cho tầng Mạng (Network) gọi khi nhận được lệnh đặt giá.
     *
     * Cách hoạt động:
     * - Khi Network nhận được packet đặt giá, nó chỉ việc gọi hàm này rồi "quên" đi (Fire and Forget).
     * - Dựa vào thuật toán Hashing (sessionId.hashCode() % PoolSize), Request này sẽ được
     *   điều hướng và thả vào đúng Hàng đợi (BlockingQueue) của 1 Worker Thread duy nhất.
     * - Cam kết hiệu năng: Phương thức này thực thi với độ phức tạp O(1), trả về (return)
     *   NGAY LẬP TỨC. Tuyệt đối không làm block luồng Virtual Thread đang giữ kết nối của Client.
     *
     * @param request  Dữ liệu thô đóng gói từ Client (chứa sessionId và bidAmount).
     * @param bidderId ID của người dùng thực hiện đặt giá (đã được bóc tách và xác thực từ Token).
     */
    void enqueueBid(BidRequestDTO request, String bidderId);

    /**
     * TÁC VỤ SCHEDULING: Dành cho tầng Service gọi khi một phiên đấu giá chính thức BẮT ĐẦU (Chuyển sang RUNNING).
     *
     * Cách hoạt động:
     * - Hàm này không tạo Thread vòng lặp (vì Worker Threads đã chạy sẵn lúc bật Server).
     * - Nhiệm vụ duy nhất của nó là tính toán khoảng thời gian từ "Hiện tại" đến "Lúc kết thúc phiên" (endTime).
     * - Sau đó, nó đưa một Nhiệm vụ (Task) đếm ngược vào hệ thống ScheduledExecutorService.
     * - Đúng khoảnh khắc hết giờ, Task sẽ tự động nổ, kích hoạt quy trình đóng phiên (khóa queue,
     *   cập nhật DB, phát thanh kết quả).
     * - Lưu ý (Dành cho Dev Core): Hàm này cũng được gọi lại nội bộ khi xảy ra Anti-sniping
     *   để gia hạn lại đồng hồ đếm ngược.
     *
     * @param sessionId ID của phiên đấu giá cần lên lịch đếm ngược.
     */
    void startSessionProcessor(String sessionId);
}