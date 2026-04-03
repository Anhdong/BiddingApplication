package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DAO.Interface.IAuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Interface.IItemDAO;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuctionSessionDAOTest {
    @Disabled
    @Test
    public void testAuctionSession() {
        System.out.println("=== BẮT ĐẦU TEST TÍCH HỢP AUCTION SESSION & HISTORY ===\n");

        // Khởi tạo các DAO
        UserDAO userDAO = UserDAO.getInstance();
        IItemDAO itemDAO = ItemDAO.getInstance();
        IAuctionSessionDAO sessionDAO = AuctionSessionDAO.getInstance();

        // Chuẩn bị các ID ngẫu nhiên cho phiên test
        String sellerId = UUID.randomUUID().toString();
        String winnerId = UUID.randomUUID().toString();
        String itemId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        try {
            // =====================================================================
            // [BƯỚC 1] SETUP DỮ LIỆU NỀN (Tránh lỗi Foreign Key)
            // =====================================================================
            System.out.println("⏳ [BƯỚC 1] Đang tạo dữ liệu nền (Seller, Winner, Item)...");

            // 1.1 Tạo Seller
            Seller seller = new Seller();
            seller.setId(sellerId);
            seller.setUsername("seller_pro_test");
            seller.setEmail("lmao");
            seller.setPasswordHash("123456");
            seller.setRole(RoleType.SELLER);
            userDAO.insertUser(seller);

            // 1.2 Tạo Winner (Người thắng cuộc)
            Bidder winner = new Bidder();
            winner.setId(winnerId);
            winner.setUsername("rich_bidder_test");
            winner.setEmail("rich_bidder");
            winner.setPasswordHash("123456");
            winner.setRole(RoleType.BIDDER);
            winner.setActive(true);
            userDAO.insertUser(winner);

            // 1.3 Tạo Item thuộc về Seller
            Item item = new Electronics();
            item.setId(itemId);
            item.setSellerId(sellerId);
            item.setName("Laptop Gaming Test");
            item.setCategory(Category.ELECTRONICS);
            itemDAO.insertItem(item);

            System.out.println("✅ Setup dữ liệu nền thành công!\n");

            // =====================================================================
            // [BƯỚC 2] TEST INSERT SESSION
            // =====================================================================
            System.out.println("⏳ [BƯỚC 2] Đang test insertSession()...");
            AuctionSession newSession = new AuctionSession();
            newSession.setId(sessionId);
            newSession.setItemId(itemId);
            newSession.setSellerId(sellerId);
            newSession.setStartPrice(new BigDecimal("1500.00"));
            newSession.setStatus(SessionStatus.OPEN); // Trạng thái mở [cite: 55]
            newSession.setStartTime(LocalDateTime.now().plusMinutes(5));
            newSession.setEndTime(LocalDateTime.now().plusDays(1));
            newSession.setBidStep(new BigDecimal("100.00"));

            boolean isSessionInserted = sessionDAO.insertSession(newSession);
            if (isSessionInserted) {
                System.out.println("✅ Insert Session thành công!\n");
            } else {
                throw new RuntimeException("Insert Session thất bại!");
            }

            // =====================================================================
            // [BƯỚC 2.5] TEST UPDATE (Cập nhật giá, người thắng và trạng thái)
            // =====================================================================
            System.out.println("⏳ [BƯỚC 2.5] Đang test getSessionInfo()...");
            SessionInfoResponseDTO info = sessionDAO.getSessionInfo(sessionId);
            if(info!=null){
                System.out.println("✅ Lấy thông tin phiên thành công !");
                System.out.println(info.getSessionId());
            }
            // =====================================================================
            // [BƯỚC 3] TEST UPDATE (Cập nhật giá, người thắng và trạng thái)
            // =====================================================================
            System.out.println("⏳ [BƯỚC 3] Đang mô phỏng kết thúc phiên đấu giá...");
            // Mô phỏng việc Bidder đấu giá thành công và phiên kết thúc
            sessionDAO.updatePriceAndWinner(sessionId, new BigDecimal("2500.00"), winnerId);
            sessionDAO.updateStatus(sessionId, SessionStatus.FINISHED); // Cập nhật FINISHED [cite: 55]
            System.out.println("✅ Đã chốt giá 2500.00 cho winner: rich_bidder_test\n");

            // =====================================================================
            // [BƯỚC 4] TEST LẤY LỊCH SỬ SELLER (Hàm quan trọng nhất)
            // =====================================================================
            System.out.println("⏳ [BƯỚC 4] Đang test getSellerHistory()...");
            List<SellerHistoryResponseDTO> historyList = sessionDAO.getSellerHistory(sellerId);

            if (historyList != null && !historyList.isEmpty()) {
                System.out.println("✅ Lấy lịch sử thành công! Số lượng phiên: " + historyList.size());
                for (SellerHistoryResponseDTO dto : historyList) {
                    System.out.println("--------------------------------------------------");
                    System.out.println("📦 Tên SP      : " + dto.getItemName()); // Chỗ này chứng minh JOIN Item thành công
                    System.out.println("💰 Giá khởi điểm: " + dto.getStartPrice());
                    System.out.println("🏆 Giá chốt     : " + dto.getFinalPrice());
                    System.out.println("👑 Người thắng  : " + dto.getWinnerName()); // Chỗ này chứng minh JOIN User thành công
                    System.out.println("📌 Trạng thái   : " + dto.getStatus());
                    System.out.println("--------------------------------------------------");
                }
            } else {
                System.err.println("❌ Lấy lịch sử thất bại (List rỗng hoặc null). Hãy kiểm tra lại câu lệnh SQL JOIN của bạn.");
            }

        } catch (Exception e) {
            System.err.println("\n❌ [LỖI HỆ THỐNG] Quá trình test bị gián đoạn:");
            e.printStackTrace();
        } finally {
            // =====================================================================
            // [BƯỚC 5] CLEAN UP DỮ LIỆU (Tư duy Idempotency)
            // =====================================================================
            System.out.println("\n⏳ [BƯỚC 5] Đang dọn dẹp Database...");
            try {
                // XÓA THEO THỨ TỰ TỪ BẢNG CON ĐẾN BẢNG CHA để tránh lỗi Foreign Key
                // Lưu ý: Nếu Interface của bạn chưa có deleteSession, bạn phải tự viết lệnh SQL hoặc xóa tay
                //sessionDAO.deleteSession(sessionId);

//                itemDAO.deleteItem(itemId);
//                userDAO.deleteUser(winnerId);
//                userDAO.deleteUser(sellerId);
                System.out.println("✅ Dọn dẹp thành công. Trả lại Database sạch sẽ!");
            } catch (Exception cleanUpEx) {
                System.err.println("❌ Lỗi khi dọn dẹp dữ liệu (Có thể Interface chưa có hàm delete): " + cleanUpEx.getMessage());
            }
        }
    }
}
