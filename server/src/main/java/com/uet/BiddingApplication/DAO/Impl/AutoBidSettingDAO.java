package com.uet.BiddingApplication.DAO.Impl;
import com.uet.BiddingApplication.DAO.Interface.IAutoBidSettingDAO;
import com.uet.BiddingApplication.Model.AutoBidSetting;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AutoBidSettingDAO implements IAutoBidSettingDAO {

    // Khởi tạo Logger SLF4J
    private static final Logger log = LoggerFactory.getLogger(AutoBidSettingDAO.class);

    private static volatile AutoBidSettingDAO instance;

    private AutoBidSettingDAO() {}

    public static AutoBidSettingDAO getInstance() {
        if (instance == null) {
            synchronized (AutoBidSettingDAO.class) {
                if (instance == null) {
                    instance = new AutoBidSettingDAO();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean upsertAutoBid(AutoBidSetting autoBid) {
        // Cú pháp PostgreSQL Upsert: Nếu trùng cặp (session_id, bidder_id) thì tiến hành UPDATE giá trị mới
        String sql = "INSERT INTO auto_bid_settings (id, session_id, bidder_id, max_bid, increment, created_at) " +
                "VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?) " +
                "ON CONFLICT (session_id, bidder_id) " +
                "DO UPDATE SET " +
                "   max_bid = EXCLUDED.max_bid, " +
                "   increment = EXCLUDED.increment, " +
                "   created_at = EXCLUDED.created_at";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Vẫn sinh UUID và Timestamp nếu đây là lần cài đặt đầu tiên (Insert)
            String id = (autoBid.getId() != null) ? autoBid.getId() : UUID.randomUUID().toString();
            Timestamp createdAt = (autoBid.getCreatedAt() != null) ? Timestamp.valueOf(autoBid.getCreatedAt()) : new Timestamp(System.currentTimeMillis());

            ps.setString(1, id);
            ps.setString(2, autoBid.getSessionId());
            ps.setString(3, autoBid.getBidderId());
            ps.setBigDecimal(4, autoBid.getMaxBid());
            ps.setBigDecimal(5, autoBid.getIncrement());
            ps.setTimestamp(6, createdAt);

            // executeUpdate sẽ trả về số lượng dòng bị tác động (Insert = 1, Update = 1 hoặc 2 tùy hệ quản trị, nhưng > 0 là thành công)
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("[AutoBidSettingDAO] Lỗi upsertAutoBid: ", e);
            return false;
        }
    }

    @Override
    public boolean deleteAutoBid(String bidderId, String sessionId) {
        String sql = "DELETE FROM auto_bid_settings WHERE bidder_id = ?::uuid AND session_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            ps.setString(2, sessionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("[AutoBidSettingDAO] Lỗi deleteAutoBid: ", e);
            return false;
        }
    }

    @Override
    public AutoBidSetting getAutoBid(String bidderId, String sessionId) {
        String sql = "SELECT id, session_id, bidder_id, max_bid, increment, created_at " +
                "FROM auto_bid_settings WHERE bidder_id = ?::uuid AND session_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            ps.setString(2, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAutoBid(rs);
                }
            }
        } catch (SQLException e) {
            log.error("[AutoBidSettingDAO] Lỗi getAutoBid: ", e);
        }
        return null;
    }

    @Override
    public boolean deleteAllBySessionId(String sessionId) {
        String sql = "DELETE FROM auto_bid_settings WHERE session_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            log.error("[AutoBidSettingDAO] Lỗi deleteAllBySessionId: ", e);
            return false;
        }
    }
    @Override
    public List<AutoBidSetting> getAllAutoBids() {
        List<AutoBidSetting> list = new ArrayList<>();
        // Truy vấn toàn bộ cấu hình hiện có trong hệ thống
        String sql = "SELECT id, session_id, bidder_id, max_bid, increment, created_at FROM auto_bid_settings";

        // Sử dụng try-with-resources đóng gom nhóm cả Connection, PreparedStatement và ResultSet
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToAutoBid(rs));
            }
        } catch (SQLException e) {
            // Log lỗi chuẩn SLF4J đi kèm Stack Trace
            log.error("[AutoBidSettingDAO] Lỗi getAllAutoBids: ", e);
        }
        return list;
    }
    @Override
    public boolean deleteAllByBidderId(String bidderId) {
        // Xóa tất cả các bản ghi autobid thuộc về một bidder_id cụ thể trên toàn hệ thống
        String sql = "DELETE FROM auto_bid_settings WHERE bidder_id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);

            // Tương tự deleteAllBySessionId, executeUpdate() trả về true nếu không có Exception.
            // Dù người dùng chưa từng cài auto-bid (xóa 0 dòng), lệnh vẫn tính là thành công.
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            // Ghi nhận lỗi chi tiết qua SLF4J
            log.error("[AutoBidSettingDAO] Lỗi deleteAllByBidderId: ", e);
            return false;
        }
    }
    // Helper method dùng nội bộ trong DAO để map dữ liệu, ném SQLException ra cho hàm gọi xử lý
    private AutoBidSetting mapRowToAutoBid(ResultSet rs) throws SQLException {
        AutoBidSetting autoBid = new AutoBidSetting();

        autoBid.setId(rs.getString("id"));
        autoBid.setSessionId(rs.getString("session_id"));
        autoBid.setBidderId(rs.getString("bidder_id"));
        autoBid.setMaxBid(rs.getBigDecimal("max_bid"));
        autoBid.setIncrement(rs.getBigDecimal("increment"));

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            autoBid.setCreatedAt(createdAtTs.toLocalDateTime());
        }

        return autoBid;
    }
}