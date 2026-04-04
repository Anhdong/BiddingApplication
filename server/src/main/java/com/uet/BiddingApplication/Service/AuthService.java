package com.uet.BiddingApplication.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Request.PasswordChangeRequestDTO;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;

/**
 * Lớp xử lý nghiệp vụ xác thực người dùng (Đăng nhập, Đăng ký, Quản lý phiên).
 * Áp dụng mẫu thiết kế Singleton.
 */
public class AuthService {

    // 1. Khởi tạo Singleton
    private static volatile AuthService instance;

    // 2. Thuộc tính nội bộ
    // Gọi DAO từ Tech Lead để giao tiếp DB
    private UserDAO userDAO;

    // Lưu trữ RAM: Key là token, Value là userId
    private ConcurrentHashMap<String, String> userCache;

    // Constructor private để ngăn khởi tạo từ bên ngoài
    private AuthService() {
        this.userDAO = UserDAO.getInstance(); // Giả định Tech Lead dùng Singleton cho DAO
        this.userCache = new ConcurrentHashMap<>();
    }

    // Lấy instance duy nhất của AuthService
    public static AuthService getInstance() {
        // Kiểm tra lần 1: Bỏ qua khóa nếu đối tượng đã được tạo
        if (instance == null) {
            // Chỉ khóa class tại thời điểm khởi tạo lần đầu tiên
            synchronized (AuthService.class) {
                // Kiểm tra lần 2: Tránh trường hợp 2 luồng cùng lọt qua lần kiểm tra 1
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    // =======================================================================
    // CÁC PHƯƠNG THỨC XỬ LÝ NGHIỆP VỤ (BUSINESS LOGIC)
    // =======================================================================

    /**
     * Xử lý đăng nhập, tạo token và kiểm tra đăng nhập trùng lặp.
     */
    public UserProfileDTO login(AuthRequestDTO request) {
        // TODO 1: Dùng userDAO tìm User theo request.getEmail()
        // TODO 2: Kiểm tra mật khẩu bằng jBCrypt (So sánh request.getPassword() và hashed password trong DB)

        // TODO 3: Nếu đúng mật khẩu -> Lấy userId
        String userId = "lấy_từ_user_tìm_được";

        // TODO 4: Lấy danh sách client đang online từ AuctionServer (Thành viên 2)
        // Kiểm tra xem userId này có đang kết nối không. Nếu có:
        // oldHandler.forceClose("Tài khoản đã đăng nhập ở nơi khác");

        // TODO 5: Tạo mã Token UUID mới
        String token = UUID.randomUUID().toString();

        // TODO 6: Lưu vào RAM để quản lý phiên
        userCache.put(token, userId);

        // TODO 7: Đóng gói dữ liệu User vào UserProfileDTO và trả về
        return new UserProfileDTO(/* truyền thông tin vào đây */);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public void register(RegisterRequestDTO request) {
        // TODO 1: Băm mật khẩu bằng jBCrypt
        // TODO 2: Khởi tạo đối tượng Entity User mới
        // TODO 3: Gọi userDAO.insert(newUser) để lưu xuống DB
    }

    /**
     * Kiểm tra tính hợp lệ của token và trả về userId tương ứng.
     */
    public String validateToken(String token) {
        // Rất đơn giản: Lấy userId từ HashMap. Nếu token sai/hết hạn, nó tự trả về null.
        if (token == null) return null;
        return userCache.get(token);
    }

    /**
     * Xử lý đăng xuất (Xóa phiên làm việc).
     */
    public void logout(String token) {
        if (token != null) {
            userCache.remove(token); // Xóa token khỏi RAM
        }
    }

    /**
     * Thay đổi mật khẩu người dùng.
     */
    public void changePassword(PasswordChangeRequestDTO request, String userId) {
        // TODO 1: Lấy thông tin user hiện tại từ DB bằng userId
        // TODO 2: Xác thực mật khẩu cũ
        // TODO 3: Băm mật khẩu mới và gọi userDAO.changePassword(...)
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân.
     */
    public void updateProfile(ProfileUpdateRequestDTO request, String userId) {
        // TODO: Map dữ liệu từ request sang đối tượng User và gọi userDAO.updateProfile(...)
    }
}