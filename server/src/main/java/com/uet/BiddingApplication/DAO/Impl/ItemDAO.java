package com.uet.BiddingApplication.DAO.Impl;

import com.uet.BiddingApplication.DAO.Interface.IItemDAO;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO implements IItemDAO {

    // --- Singleton Pattern ---
    private static volatile ItemDAO instance;

    private ItemDAO() {}

    public static ItemDAO getInstance() {
        if (instance == null) {
            synchronized (ItemDAO.class) {
                if (instance == null) {
                    instance = new ItemDAO();
                }
            }
        }
        return instance;
    }
    // -------------------------

    @Override
    public boolean insertItem(Item item) {
        // Cột "condition" thường là từ khóa nhạy cảm trong SQL, tùy DB có thể cần đổi tên thành "item_condition"
        String sql = "INSERT INTO items (id, name, description, image_url, seller_id, category, warranty_months, artist_name, condition, created_at) " +
                "VALUES (?::uuid, ?, ?, ?, ?::uuid, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Các trường cơ bản (Entity & Item cha)
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getImageURL());
            ps.setString(5, item.getSellerId());

            // Xử lý đa hình (Downcasting) để lấy trường đặc thù
            String category = "OTHERS";
            Integer warrantyMonths = null;
            String artistName = null;
            String condition = null;

            if (item instanceof Electronics) {
                category = "ELECTRONICS";
                warrantyMonths = ((Electronics) item).getWarrantyMonths();
            } else if (item instanceof Art) {
                category = "ART";
                artistName = ((Art) item).getArtistName();
            } else if (item instanceof Vehicle) {
                category = "VEHICLE";
                condition = ((Vehicle) item).getCondition();
            }

            ps.setString(6, category);

            // Xử lý cẩn thận kiểu Integer/String có thể null
            if (warrantyMonths != null) ps.setInt(7, warrantyMonths);
            else ps.setNull(7, java.sql.Types.INTEGER);

            ps.setString(8, artistName);
            ps.setString(9, condition);
            ps.setTimestamp(10, Timestamp.valueOf(item.getCreatedAt()));

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi insertItem: " + item.getName());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateItem(Item item) {
        // Lưu ý: Thường ta không cho phép đổi người bán (seller_id) hay category
        String sql = "UPDATE items SET name = ?, description = ?, image_url = ?, warranty_months = ?, artist_name = ?, condition = ? WHERE id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getImageURL());

            if (item instanceof Electronics) {
                ps.setInt(4, ((Electronics) item).getWarrantyMonths());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }

            ps.setString(5, (item instanceof Art) ? ((Art) item).getArtistName() : null);
            ps.setString(6, (item instanceof Vehicle) ? ((Vehicle) item).getCondition() : null);

            ps.setString(7, item.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi updateItem cho ID: " + item.getId());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteItem(String itemId) {
        String sql = "DELETE FROM items WHERE id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi deleteItem ID: " + itemId);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Item getItemById(String itemId) {
        String sql = "SELECT * FROM items WHERE id = ?::uuid";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItem(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi getItemById: " + itemId);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Item> getItemsBySellerId(String sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?::uuid ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi getItemsBySellerId: " + sellerId);
            e.printStackTrace();
        }
        return items;
    }

    // --- Helper Method: Tránh lặp lại code ---
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String category = rs.getString("category");
        Item item = null;

        // Factory logic khởi tạo lớp con
        if ("ELECTRONICS".equalsIgnoreCase(category)) {
            Electronics elec = new Electronics();
            elec.setWarrantyMonths(rs.getInt("warranty_months"));
            item = elec;
        } else if ("ART".equalsIgnoreCase(category)) {
            Art art = new Art();
            art.setArtistName(rs.getString("artist_name"));
            item = art;
        } else if ("VEHICLE".equalsIgnoreCase(category)) {
            Vehicle vehicle = new Vehicle();
            vehicle.setCondition(rs.getString("condition"));
            item = vehicle;
        } else {
            item = new Others(); // Lớp con rỗng cho các đồ vật khác
        }

        // Mapping các trường chung
        item.setId(rs.getString("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setImageURL(rs.getString("image_url"));
        item.setSellerId(rs.getString("seller_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            item.setCreatedAt(createdAt.toLocalDateTime());
        }

        return item;
    }
}