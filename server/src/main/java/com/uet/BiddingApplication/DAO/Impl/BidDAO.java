package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DAO.Interface.IBidDAO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.BidType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.BidTransaction;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BidDAO implements IBidDAO {

    // Singleton Pattern
    private static volatile BidDAO instance;

    private BidDAO() {}

    public static BidDAO getInstance() {
        if (instance == null) {
            synchronized (BidDAO.class) {
                if (instance == null) {
                    instance = new BidDAO();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean insertBid(BidTransaction bid) {
        String sql = "INSERT INTO bid_transactions (id, created_at, bidder_id, session_id, bid_amount, bid_type) " +
                "VALUES (?::uuid, ?, ?::uuid, ?::uuid, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Giả định Entity BidTransaction có chứa dữ liệu hợp lệ, tạo UUID và thời gian tự động nếu thiếu
            String bidId = (bid.getId() != null) ? bid.getId() : UUID.randomUUID().toString();
            LocalDateTime createdAt = (bid.getCreatedAt() != null) ? bid.getCreatedAt() : LocalDateTime.now();

            ps.setString(1, bidId);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(createdAt));
            ps.setString(3, bid.getBidderId());
            ps.setString(4, bid.getSessionId());
            ps.setBigDecimal(5, bid.getBidAmount());
            ps.setString(6, bid.getBidType().name());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BidDAO] Lỗi insertBid: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<BidHistoryDTO> getRecentBids(String sessionId) {
        List<BidHistoryDTO> history = new ArrayList<>();

        // JOIN với bảng users để lấy username cho DTO thay vì chỉ trả về bidder_id
        String sql = "SELECT u.id AS bidder, b.bid_amount, b.created_at, b.session_id " +
                "FROM bid_transactions b " +
                "INNER JOIN users u ON b.bidder_id = u.id " +
                "WHERE b.session_id = ?::uuid " +
                "ORDER BY b.created_at DESC " +
                "LIMIT 10";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidHistoryDTO dto = new BidHistoryDTO();
                    String winnerId = rs.getString("bidder")!=null?rs.getString("bidder").substring(0,5):null;
                    dto.setBidderName(winnerId);
                    dto.setBidAmount(rs.getBigDecimal("bid_amount"));

                    Timestamp timeTs = rs.getTimestamp("created_at");
                    if (timeTs != null) {
                        dto.setTime(timeTs.toLocalDateTime());
                    }

                    dto.setSessionId(rs.getString("session_id"));

                    history.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] Lỗi getRecentBids: " + e.getMessage());
        }

        return history;
    }

    @Override
    public List<BidderHistoryResponseDTO> getBidderHistory(String bidderId) {
        List<BidderHistoryResponseDTO> history = new ArrayList<>();

        // Cần GROUP BY session_id để lấy ra số tiền cao nhất (myHighestBid) mà user này đã cược cho TỪNG phiên
        String sql = "SELECT " +
                "   s.id AS session_id, " +
                "   i.name AS item_name, " +
                "   s.winner_id, " +
                "   MAX(b.bid_amount) AS my_highest_bid, " +
                "   s.current_price AS final_price, " +
                "   s.status, " +
                "   MAX(b.created_at) AS latest_bid_time " +
                "FROM bid_transactions b " +
                "INNER JOIN auction_sessions s ON b.session_id = s.id " +
                "INNER JOIN items i ON s.item_id = i.id " +
                "WHERE b.bidder_id = ?::uuid " +
                "GROUP BY s.id, i.name, s.winner_id, s.current_price, s.status " +
                "ORDER BY latest_bid_time DESC";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidderHistoryResponseDTO dto = new BidderHistoryResponseDTO();

                    dto.setSessionId(rs.getString("session_id"));
                    dto.setItemName(rs.getString("item_name"));
                    String winnerId = rs.getString("winner_id")!=null?rs.getString("winner_id").substring(0,5):null;
                    dto.setWinnerId(winnerId);
                    dto.setMyHighestBid(rs.getBigDecimal("my_highest_bid"));
                    dto.setFinalPrice(rs.getBigDecimal("final_price"));

                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        dto.setStatus(SessionStatus.valueOf(statusStr.toUpperCase()));
                    }

                    Timestamp timeTs = rs.getTimestamp("latest_bid_time");
                    if (timeTs != null) {
                        dto.setTime(timeTs.toLocalDateTime()); // Gắn với thời điểm bid cuối cùng của user trong phiên đó
                    }

                    history.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] Lỗi getBidderHistory: " + e.getMessage());
        }

        return history;
    }
    @Override
    public boolean placeBidAtomicTransaction(String sessionId, String bidderId, BigDecimal bidAmount, BidType bidType) {
        String insertBidSql = "INSERT INTO bid_transactions (id, created_at, bidder_id, session_id, bid_amount, bid_type) VALUES (?::uuid, ?, ?::uuid, ?::uuid, ?, ?)";
        String updateSessionSql = "UPDATE auction_sessions SET current_price = ?, winner_id = ?::uuid WHERE id = ?::uuid";

        // Sử dụng try-with-resources cho Connection để đảm bảo connection luôn được trả về pool
        try (Connection conn = DatabaseConnectionPool.getConnection()) {

            try {
                // 1. Tắt auto-commit để bắt đầu Transaction
                conn.setAutoCommit(false);

                // 2. Thực thi lệnh Insert vào bid_transactions
                try (PreparedStatement ps = conn.prepareStatement(insertBidSql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    ps.setString(3, bidderId);
                    ps.setString(4, sessionId);
                    ps.setBigDecimal(5, bidAmount);
                    ps.setString(6, bidType.name());

                    ps.executeUpdate();
                }

                // 3. Thực thi lệnh Update lên auction_sessions
                try (PreparedStatement ps = conn.prepareStatement(updateSessionSql)) {
                    ps.setBigDecimal(1, bidAmount);
                    ps.setString(2, bidderId);
                    ps.setString(3, sessionId);

                    ps.executeUpdate();
                }

                // 4. Nếu mọi thứ thành công, chốt Transaction (Commit)
                conn.commit();
                return true;

            } catch (SQLException e) {
                // 5. Nếu có bất kỳ Exception nào (lỗi SQL, constraint, khóa bảng, v.v.), Rollback ngay lập tức
                conn.rollback();
                System.err.println("[BidDAO] Lỗi thực thi transaction, đã rollback hoàn toàn: " + e.getMessage());
                return false;
            } finally {
                // 6. LUÔN LUÔN trả lại trạng thái nguyên bản cho connection trước khi đưa nó về Hikari Pool
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[BidDAO] Lỗi kết nối hoặc lỗi cấu hình transaction tại placeBidAtomicTransaction: " + e.getMessage());
            return false;
        }
    }
}