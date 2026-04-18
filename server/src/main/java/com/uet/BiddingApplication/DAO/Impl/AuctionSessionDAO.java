package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DAO.Interface.IAuctionSessionDAO;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionSessionDAO implements IAuctionSessionDAO {

    private static volatile AuctionSessionDAO instance;

    private AuctionSessionDAO() {}

    public static AuctionSessionDAO getInstance() {
        if (instance == null) {
            synchronized (AuctionSessionDAO.class) {
                if (instance == null) {
                    instance = new AuctionSessionDAO();
                }
            }
        }
        return instance;
    }


    @Override
    public boolean insertSession(AuctionSession session) {
        String sql = "INSERT INTO auction_sessions (id, item_id, seller_id, start_time, end_time, status, start_price, current_price, bid_step, created_at) " +
                "VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, session.getId());
            ps.setString(2, session.getItemId());
            ps.setString(3, session.getSellerId());
            ps.setTimestamp(4, Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(session.getEndTime()));
            ps.setString(6, session.getStatus().name());
            ps.setBigDecimal(7, session.getStartPrice());
            ps.setBigDecimal(8, session.getCurrentPrice());
            ps.setBigDecimal(9, session.getBidStep());
            ps.setTimestamp(10, Timestamp.valueOf(session.getCreatedAt()));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionSessionDAO] Lỗi insertSession: " + session.getId());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public AuctionSession getSessionByItemId(String ItemId) {
        String sql = "SELECT * FROM auction_sessions WHERE item_id = ?::uuid";
        return findSession(sql, ItemId);
    }

    @Override
    public AuctionSession getSessionById(String id) {
        String sql = "SELECT * FROM auction_sessions WHERE id = ?::uuid";
        return findSession(sql, id);
    }

    public AuctionSession findSession(String sql,String parameter) {
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToSession(rs);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionSessionDAO] Lỗi getSessionById: " + parameter);
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public boolean updatePriceAndWinner(String sessionId, BigDecimal newPrice, String winnerId) {
        // Lưu ý: WinnerId có thể null nếu cập nhật giá khởi điểm, nhưng thường hàm này gọi khi có Bid mới.
        String sql = "UPDATE auction_sessions SET current_price = ?, winner_id = ?::uuid WHERE id = ?::uuid";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, newPrice);
            if (winnerId != null) {
                ps.setString(2, winnerId);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR); // Set NULL một cách an toàn
            }
            ps.setString(3, sessionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionSessionDAO] Lỗi updatePriceAndWinner cho Session: " + sessionId);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateStatus(String sessionId, SessionStatus status) {
        String sql = "UPDATE auction_sessions SET status = ? WHERE id = ?::uuid";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, sessionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionSessionDAO] Lỗi updateStatus: " + sessionId);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<AuctionSession> getAllSessions(boolean isActive) {
        List<AuctionSession> list = new ArrayList<>();
        String sql;
        if(isActive) {
            sql= "SELECT * FROM auction_sessions WHERE status in ('OPEN','RUNNING') ORDER BY end_time ASC";
        }
        else sql = "SELECT * FROM auction_sessions ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToSession(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AuctionSessionDAO] Lỗi getAllSessions");
            e.printStackTrace();
        }
        return list;
    }


    // ====================================================================================
    // HÀM BÁO CÁO LỊCH SỬ (DÙNG TỐI ƯU JOIN SQL VÀ TRẢ VỀ DTO)
    // ====================================================================================
    @Override
    public List<SellerHistoryResponseDTO> getSellerHistory(String sellerId) {
       List<SellerHistoryResponseDTO> list = new ArrayList<>();
       String sql = "WITH FilteredSessions AS (\n" +
               "    -- Lọc dữ liệu thu gọn trước\n" +
               "    SELECT id, item_id, winner_id, start_price, current_price, start_time, end_time, status, created_at\n" +
               "    FROM auction_sessions\n" +
               "    WHERE seller_id = ?::uuid AND status IN ('FINISHED', 'CANCELED')\n" +
               ")\n" +
               "-- Sau đó mới mang đi JOIN\n" +
               "SELECT \n" +
               "    a.id,i.name, a.start_price, a.current_price, \n" +
               "    a.start_time, a.end_time, u.username, a.status\n" +
               "FROM FilteredSessions a\n" +
               "JOIN items i ON i.id = a.item_id\n" +
               "LEFT JOIN users u ON u.id = a.winner_id\n" +
               "ORDER BY a.created_at DESC;";
       try(Connection conn=DatabaseConnectionPool.getConnection();
           PreparedStatement ps=conn.prepareStatement(sql)) {
           ps.setString(1, sellerId);
           try(ResultSet rs=ps.executeQuery()) {
               while (rs.next()) {
                   SellerHistoryResponseDTO dto=new SellerHistoryResponseDTO();
                   dto.setSessionId(rs.getString("id"));
                   dto.setStatus(SessionStatus.valueOf(rs.getString("status")));
                   dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                   dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                   dto.setStartPrice(rs.getBigDecimal("start_price"));
                   dto.setFinalPrice(rs.getBigDecimal("current_price"));
                   dto.setItemName(rs.getString("name"));
                   dto.setWinnerName(rs.getString("username"));

                   list.add(dto);
               }
           }
       }
       catch (SQLException e){
           System.err.println("[AuctionSessionDAO] Lỗi getSellerHistory");
           e.printStackTrace();
       }
        return list;
    }
    @Override
    public SessionInfoResponseDTO getSessionInfo(String sessionId){
        String sql="with item_info as(\n" +
                "  select i.name,i.seller_id,i.description,i.image_url,i.category,i.condition,i.artist_name,i.warranty_months,\n" +
                "         a.id,a.status,a.start_price,a.start_time,a.end_time\n" +
                "  from items i\n" +
                "  join auction_sessions a on i.id=a.item_id\n" +
                "  where a.id=?::uuid\n" +
                ")\n" +
                "select info.name,info.description,info.image_url,info.category,info.condition,info.artist_name,info.warranty_months,\n" +
                "      info.id,info.status,info.start_price,info.start_time,info.end_time,u.username\n" +
                "from item_info info\n" +
                "join users u on u.id=info.seller_id;";
        SessionInfoResponseDTO dto=new SessionInfoResponseDTO();
        try(Connection conn=DatabaseConnectionPool.getConnection();
        PreparedStatement ps=conn.prepareStatement(sql);){
            ps.setString(1, sessionId);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){
                dto.setSessionId(rs.getString("id"));
                dto.setStatus(SessionStatus.valueOf(rs.getString("status")));
                dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                dto.setStartPrice(rs.getBigDecimal("start_price"));
                dto.setDescription(rs.getString("description"));
                dto.setImageUrl(rs.getString("image_url"));
                dto.setItemName(rs.getString("name"));
                dto.setSellerName(rs.getString("username"));
                dto.setCategory(Category.valueOf(rs.getString("category")));
                switch (dto.getCategory()) {
                    case Category.ART:
                        dto.setAttribute(rs.getString("artist_name"));
                        break;
                    case Category.ELECTRONICS:
                        dto.setAttribute(rs.getString("warranty_months"));
                        break;
                    case VEHICLE:
                        dto.setAttribute(rs.getString("condition"));
                        break;
                }
            }

        }catch (SQLException e){
            System.err.println("[AuctionSessionDAO] Lỗi getSessionInfo");
            e.printStackTrace();
        }
        return dto;
    }

    // --- Helper Method ---
    private AuctionSession mapRowToSession(ResultSet rs) throws SQLException {
        AuctionSession session = new AuctionSession();
        session.setId(rs.getString("id"));
        session.setItemId(rs.getString("item_id"));
        session.setSellerId(rs.getString("seller_id"));
        session.setWinnerId(rs.getString("winner_id"));

        // Thời gian (Timestamp -> LocalDateTime)
        session.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        session.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());

        session.setStatus(SessionStatus.valueOf(rs.getString("status")));
        session.setStartPrice(rs.getBigDecimal("start_price"));
        session.setCurrentPrice(rs.getBigDecimal("current_price"));
        session.setBidStep(rs.getBigDecimal("bid_step"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            session.setCreatedAt(createdAt.toLocalDateTime());
        }
        return session;
    }
}
