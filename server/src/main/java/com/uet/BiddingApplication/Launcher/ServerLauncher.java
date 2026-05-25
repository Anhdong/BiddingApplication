package com.uet.BiddingApplication.Launcher;

import com.uet.BiddingApplication.Config.AppConfig;
import com.uet.BiddingApplication.CoreService.InMemoryBidServiceImpl;
import com.uet.BiddingApplication.CoreService.SearchCacheManager;

import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AutoBidSettingDAO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.AutoBidSetting;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import com.uet.BiddingApplication.Service.AutoBidManager;

import java.util.List;

/**
 * Class khởi động chính cho hệ thống đấu giá.
 * Đảm bảo tính chuyên nghiệp, chính xác và thực thi theo đúng trình tự kiến trúc.
 */
public class ServerLauncher {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServerLauncher.class);

    public static void main(String[] args) {
        log.info("  KHỞI ĐỘNG HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - UET    ");
        try {
            // BƯỚC 1: KHỞI TẠO TẦNG DỮ LIỆU (DATABASE & CACHE)
            log.info("[1/4] Đang nạp dữ liệu lên RAM...");
            AppConfig.loadConfig();
            SearchCacheManager cacheManager = SearchCacheManager.getInstance();

            // Nạp toàn bộ phiên OPEN/RUNNING và Items tương ứng từ DAO
            cacheManager.loadInitialData();
            cacheManager.loadInitialItems();
            log.info(" HOÀN TẤT.");

            // BƯỚC 2: KHÔI PHỤC TRẠNG THÁI HỆ THỐNG (RECOVERY LOGIC)
            log.info("[2/4] Đang khôi phục các luồng xử lý phiên...");

            // Khôi phục autoBid cho các phiên hiện tại
            List<AutoBidSetting> activeAutoBids = AutoBidSettingDAO.getInstance().getAllAutoBids();
            for (AutoBidSetting setting : activeAutoBids) {
                AutoBidManager.getInstance().registerAutoBid(setting);
            }

            // Lấy danh sách thô từ Cache vừa nạp
            List<AuctionSession> allRunningSessions = cacheManager.getSessionsByStatus(SessionStatus.RUNNING);

            for (AuctionSession session : allRunningSessions) {
                // Trường hợp 1: Phiên đang chạy (RUNNING) - Cần kích hoạt lại bộ đếm ngược kết thúc
                if (session.getStatus() == SessionStatus.RUNNING) {
                    InMemoryBidServiceImpl.getInstance().startSessionProcessor(session.getId());
                }
            }

            // Trường hợp 2: Các phiên chưa bắt đầu (OPEN) - Đưa vào Scheduler khởi động
            SessionStartScheduler.getInstance().loadAllPendingSessions();
            log.info(" HOÀN TẤT.");

            // BƯỚC 3: KIỂM TRA SẴN SÀNG HỆ THỐNG LÕI (CORE CHECK)
            log.info("[3/4] Kiểm tra Worker Pool (Fixed 8 Threads)...");
            // Đảm bảo INSTANCE của Bid Service đã được khởi tạo và các luồng đang WAITING
            InMemoryBidServiceImpl.getInstance();
            log.info(" SẴN SÀNG.");

            // BƯỚC 4: MỞ CỔNG MẠNG (NETWORK LAYER)
            int port = AppConfig.getServerPort(); // Cổng mặc định của hệ thống
            log.info("[4/4] Đang mở cổng mạng: " + port);
            AuctionServer server=AuctionServer.getInstance();
            // NDJSON & Virtual Threads sẽ được quản lý bên trong start()
            log.info(">>> SERVER ĐANG LẮNG NGHE...");
            log.info("--------------------------------------------------");

            server.start(port);

        } catch (Exception e) {
            log.error("\n[LỖI NGHIÊM TRỌNG] Khởi động hệ thống thất bại!");
            log.error("Chi tiết: " + e.getMessage());
            log.error("Đã xảy ra lỗi Exception:", e);
            System.exit(1); // Dừng chương trình ngay lập tức nếu có lỗi khởi động
        }
    }
}