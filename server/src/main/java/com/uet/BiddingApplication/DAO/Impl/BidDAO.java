package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DAO.Interface.IBidDAO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.BidTransaction;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;

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
        String sql = "SELECT u.username AS bidder_name, b.bid_amount, b.created_at, b.session_id " +
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

                    dto.setBidderName(rs.getString("bidder_name"));
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
                    dto.setWinnerId(rs.getString("winner_id"));
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
}