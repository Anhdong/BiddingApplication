package com.uet.BiddingApplication.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Request.PasswordChangeRequestDTO;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.Utils.Mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;

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
        // Kiểm tra thông tin người dùng từ DAO
        User user = UserDAO.getInstance().findByUsername(request.getUsername());

        // Validate lỗi nghiệp vụ: Sai tài khoản hoặc mật khẩu
        if (user == null || !checkPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Tài khoản không tồn tại hoặc sai mật khẩu.");
        }

        // Validate trạng thái tài khoản [cite: 34]
        if (!user.isActive()) {
            throw new BusinessException("Tài khoản của bạn đã bị Admin khóa.");
        }

        // Logic xử lý đăng nhập đa thiết bị [cite: 345, 346]
        String userId = String.valueOf(user.getId());
        ClientConnectionHandler oldHandler = AuctionServer.getInstance().getClientHandler(userId);
        if (oldHandler != null) {
            oldHandler.forceClose("Tài khoản đã đăng nhập ở nơi khác");
        }

        // Tạo Token và lưu vào Cache RAM [cite: 344]
        String token = UUID.randomUUID().toString();
        userCache.put(token, userId);

        // Map Entity sang DTO và trả về (Router sẽ đóng gói ResponsePacket) [cite: 423]
        return UserMapper.toDto(user);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public void register(RegisterRequestDTO request) {
        // Validate các lỗi nghiệp vụ sâu cần truy vấn DB [cite: 34]
        if (UserDAO.getInstance().findByEmail(request.getEmail()) != null){
            throw new BusinessException("Email này đã được đăng ký.");
        }
        if (UserDAO.getInstance().findByUsername(request.getUsername()) != null){
            throw  new BusinessException("Tên đăng nhập (User Name) đã tồn tại.");
        }
        // TODO 1: Băm mật khẩu bằng jBCrypt
        String hashedPassWord = hashPassword(request.getPassword());
        // TODO 2: Khởi tạo đối tượng Entity User mới
        User newUser = UserMapper.toEntity(request);
        newUser.setPasswordHash(hashedPassWord);
        // TODO 3: Gọi userDAO.insert(newUser) để lưu xuống DB
        boolean success = userDAO.insertUser(newUser);

        if (!success) {
            throw new BusinessException("Lỗi hệ thống khi tạo tài khoản.");
        }
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
        User user = UserDAO.getInstance().findById(userId);
        // TODO 2: Xác thực mật khẩu cũ
        if (!BCrypt.checkpw(request.getOldPassword(), user.getPasswordHash())){
            throw new BusinessException("Mật khẩu cũ không chính xác.");
        }
        // TODO 3: Băm mật khẩu mới và gọi userDAO.changePassword(...)

        userDAO.changePassword(userId, hashPassword(request.getNewPassword()));
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân.
     */
    public void updateProfile(ProfileUpdateRequestDTO request, String userId) {
        // TODO: Map dữ liệu từ request sang đối tượng User và gọi userDAO.updateProfile(...)
        User user = userDAO.findById(userId);

        if (user == null){
            throw new BusinessException("Không tìm thấy thông tin người dùng.");
        }

        UserMapper.updateEntity(request, user);
        userDAO.updateProfile(user);
    }

    // --- Các phương thức bổ trợ nội bộ (Helper methods) ---
    private boolean checkPassword(String plain, String hashed) {
        // Sử dụng jBCrypt hoặc thư viện tương ứng
        return BCrypt.checkpw(plain, hashed);
    }

    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }
}