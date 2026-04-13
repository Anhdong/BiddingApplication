package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DAO.Interface.ISessionRegistrationDAO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.SessionRegistration;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SessionRegistrationDAO implements ISessionRegistrationDAO {

    // 1. Singleton Pattern
    private static volatile SessionRegistrationDAO instance;

    private SessionRegistrationDAO() {
        // Private constructor để ngăn khởi tạo từ bên ngoài
    }

    public static SessionRegistrationDAO getInstance() {
        if (instance == null) {
            synchronized (SessionRegistrationDAO.class) {
                if (instance == null) {
                    instance = new SessionRegistrationDAO();
                }
            }
        }
        return instance;
    }

    // 2. Triển khai các phương thức
    @Override
    public boolean registerBidder(SessionRegistration registration) {
        String sql = "INSERT INTO session_registrations (id,created_at,bidder_id, session_id) VALUES (?::uuid,?,?::uuid,?::uuid)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,registration.getId());
            ps.setTimestamp(2,java.sql.Timestamp.valueOf(registration.getCreatedAt()));
            ps.setString(3, registration.getBidderId());
            ps.setString(4, registration.getSessionId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[SessionRegistrationDAO] Lỗi registerBidder");
            return false;
        }
    }

    @Override
    public boolean checkRegistration(String bidderId, String sessionId) {
        String sql = "SELECT 1 FROM session_registrations WHERE bidder_id = ?::uuid AND session_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            ps.setString(2, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Trả về true nếu có bản ghi tồn tại
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[SessionRegistrationDAO] Lỗi checkRegistration");
            return false;
        }
    }

    @Override
    public boolean deleteRegistration(String bidderId, String sessionId) {
        String sql = "DELETE FROM session_registrations WHERE bidder_id = ?::uuid AND session_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            ps.setString(2, sessionId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[SessionRegistrationDAO] Lỗi deleteRegistration");
            return false;
        }
    }

    @Override
    public List<AuctionCardDTO> getRegisteredSessions(String bidderId) {
        List<AuctionCardDTO> registeredSessions = new ArrayList<>();

        // JOIN 3 bảng: session_registrations, auction_sessions và items
        String sql = "SELECT " +
                "   a.id AS session_id, " +
                "   i.name AS item_name, " +
                "   i.image_url, " +
                "   a.start_price, " +
                "   a.start_time, " +
                "   a.end_time, " +
                "   a.status " +
                "FROM session_registrations sr " +
                "INNER JOIN auction_sessions a ON sr.session_id = a.id " +
                "INNER JOIN items i ON a.item_id = i.id " +
                "WHERE sr.bidder_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuctionCardDTO dto = new AuctionCardDTO();

                    // Mapping dữ liệu
                    dto.setSessionId(rs.getString("session_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setImageURL(rs.getString("image_url"));

                    // Ép kiểu DECIMAL(19,2) sang BigDecimal
                    dto.setStartPrice(rs.getBigDecimal("start_price"));

                    // Ép kiểu DATETIME/TIMESTAMP sang LocalDateTime
                    Timestamp startTimeTs = rs.getTimestamp("start_time");
                    if (startTimeTs != null) {
                        dto.setStartTime(startTimeTs.toLocalDateTime());
                    }

                    Timestamp endTimeTs = rs.getTimestamp("end_time");
                    if (endTimeTs != null) {
                        dto.setEndTime(endTimeTs.toLocalDateTime());
                    }

                    // Map chuỗi status từ DB sang Enum
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        dto.setStatus(SessionStatus.valueOf(statusStr.toUpperCase()));
                    }

                    registeredSessions.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SessionRegistrationDAO] Lỗi getRegisteredSessions");
            e.printStackTrace();
        }

        return registeredSessions;
    }
}