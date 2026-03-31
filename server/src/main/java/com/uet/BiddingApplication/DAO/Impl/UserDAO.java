package com.uet.BiddingApplication.DAO.Impl;


import com.uet.BiddingApplication.DAO.Interface.IUserDAO;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Model.*;
import com.uet.BiddingApplication.Utils.DatabaseConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements IUserDAO {
    // Singleton Pattern
    private static volatile UserDAO instance;

    private UserDAO() {}
    public static UserDAO getInstance() {
        if (instance == null) {
            synchronized (UserDAO.class) {
                if (instance == null) {
                    instance = new UserDAO();
                }
            }
        }
        return instance;
    }
    private User mapRowToUser(ResultSet rs) throws SQLException {
        // Lấy Role để quyết định sẽ tạo Object của class con nào
        String roleStr = rs.getString("role");
        RoleType role = RoleType.valueOf(roleStr); // Giả sử Enum RoleType là: ADMIN, SELLER, BIDDER

        User user = null;

        // Dùng Factory logic nhẹ nhàng tại đây để khởi tạo đúng loại User
        switch (role) {
            case ADMIN:
                Admin admin = new Admin();
                admin.setOtpSecretKey(rs.getString("otp_secret_key"));
                user = admin;
                break;
            case SELLER:
                Seller seller = new Seller();
                seller.setBankAccount(rs.getString("bank_account"));
                user = seller;
                break;
            case BIDDER:
                Bidder bidder = new Bidder();
                bidder.setShippingAddress(rs.getString("shipping_address"));
                user = bidder;
                break;
        }

        // Nếu role không hợp lệ (không nên xảy ra nếu DB chuẩn)
        if (user == null) return null;

        // Map các trường chung của lớp cha User
        user.setId(rs.getString("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(role);
        user.setActive(rs.getBoolean("is_active"));

        // Map trường của Entity cha cùng cấp (createdAt)
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            user.setCreatedAt(createdAtTs.toLocalDateTime());
        }

        return user;
    }

    // =========================================================================
    // 1. CÁC HÀM TÌM KIẾM CƠ BẢN (READ)
    // =========================================================================
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = true";

        // Sử dụng try-with-resources tinh gọn và an toàn
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs); // Tách logic mapping ra hàm riêng biệt (SRP)
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi tìm user theo username: " + username);
            e.printStackTrace();
        }
        return null; // Trả về null nếu không tìm thấy
    }
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi findByEmail: " + email);
            e.printStackTrace();
        }
        return null;
    }

    public User findById(String userId) {
        String sql = "SELECT * FROM users WHERE id = ?::uuid";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Xử lý ép kiểu String sang UUID cho PostgreSQL
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToUser(rs);
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("[UserDAO] Lỗi findById (Sai format UUID hoặc lỗi DB): " + userId);
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================================
    // 2. CÁC HÀM THÊM / SỬA (WRITE)
    // =========================================================================

    public boolean insertUser(User user) {
        String sql = "INSERT INTO users (id, username, email, phone, password_hash, role, is_active, created_at, otp_secret_key, bank_account, shipping_address) " +
                "VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Core User Fields
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getRole().name()); // Enum to String
            ps.setBoolean(7, user.isActive());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(user.getCreatedAt())); // LocalDateTime to Timestamp

            // Xử lý đa hình cho Specific Fields (Single Table Strategy)
            String otpSecret = null, bankAccount = null, shippingAddress = null;

            if (user instanceof Admin) {
                otpSecret = ((Admin) user).getOtpSecretKey();
            } else if (user instanceof Seller) {
                bankAccount = ((Seller) user).getBankAccount();
            } else if (user instanceof Bidder) {
                shippingAddress = ((Bidder) user).getShippingAddress();
            }

            ps.setString(9, otpSecret);
            ps.setString(10, bankAccount);
            ps.setString(11, shippingAddress);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi insertUser: " + user.getUsername());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProfile(User user) {
        // Chỉ cho phép update những thông tin cơ bản, không cho phép đổi Role hay Username ở đây
        String sql = "UPDATE users SET username = ?, phone = ?, bank_account = ?, shipping_address = ? WHERE id = ?::uuid";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPhone());

            // Kiểm tra instance để set trường cụ thể
            ps.setString(3, (user instanceof Seller) ? ((Seller) user).getBankAccount() : null);
            ps.setString(4, (user instanceof Bidder) ? ((Bidder) user).getShippingAddress() : null);

            ps.setString(5, user.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi updateProfile cho User ID: " + user.getId());
            e.printStackTrace();
        }
        return false;
    }

    public boolean changePassword(String userId, String newHashedPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?::uuid";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newHashedPassword);
            ps.setString(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi changePassword cho User ID: " + userId);
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatus(String userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?::uuid";
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);
            ps.setString(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi updateStatus cho User ID: " + userId);
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // 3. CÁC HÀM QUẢN TRỊ (ADMIN)
    // =========================================================================

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi getAllUsers");
            e.printStackTrace();
        }
        return list;
    }

    public List<User> searchUsers(String keyword, String role, Boolean status) {
        List<User> list = new ArrayList<>();

        // Dùng StringBuilder để xây dựng Dynamic SQL một cách an toàn
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // Tìm kiếm tương đối (ILIKE của PostgreSQL không phân biệt hoa thường)
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (username ILIKE ? OR email ILIKE ? OR phone ILIKE ?) ");
            String searchPattern = "%" + keyword.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("ALL")) {
            sql.append("AND role = ? ");
            params.add(role.toUpperCase()); // Đảm bảo khớp với Enum
        }

        if (status != null) {
            sql.append("AND is_active = ? ");
            params.add(status);
        }

        sql.append("ORDER BY created_at DESC");

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Set params động vào PreparedStatement
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi searchUsers");
            e.printStackTrace();
        }
        return list;
    }
}