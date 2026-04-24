package com.uet.BiddingApplication.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Response.AuthResponseDTO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.DTO.Request.RegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Request.PasswordChangeRequestDTO;
import com.uet.BiddingApplication.DTO.Request.AuthRequestDTO;
import com.uet.BiddingApplication.DTO.Request.ProfileUpdateRequestDTO;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.ServerClass.AuctionServer;
import com.uet.BiddingApplication.ServerClass.ClientConnectionHandler;
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
    public AuthResponseDTO login(AuthRequestDTO request) {
        // 1. Kiểm tra thông tin người dùng từ DAO
        User user = UserDAO.getInstance().findByUsername(request.getUsername());

        if (user == null || !checkPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Tài khoản không tồn tại hoặc sai mật khẩu.");
        }

        if (!user.isActive()) {
            throw new BusinessException("Tài khoản của bạn đã bị Admin khóa.");
        }

        // 2. Logic xử lý đăng nhập đa thiết bị (Đá luồng cũ ra ngoài)
        String userId = String.valueOf(user.getId());

        // Đây là tính năng rất hay để chống 1 tài khoản đăng nhập ở 2 máy.
        ClientConnectionHandler oldHandler = AuctionServer.getInstance().getClientHandler(userId);
        if (oldHandler != null) {
            oldHandler.forceClose("Tài khoản đã được đăng nhập trên một thiết bị khác.");
        }

        // 3. Tạo Token và lưu vào Cache RAM
        String token = UUID.randomUUID().toString();
        userCache.put(token, userId);

        // 4. FIX BUG: Trả về AuthResponseDTO chứa cả Token và Profile [cite: 679]
        UserProfileDTO userProfile = UserMapper.toDto(user);
        return new AuthResponseDTO(token, userProfile);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public boolean register(RegisterRequestDTO request) {
        // 1. Sử dụng biến instance (this.userDAO) thay vì gọi tĩnh
        if (this.userDAO.findByEmail(request.getEmail()) != null) {
            throw new BusinessException("Email này đã được đăng ký.");
        }
        if (this.userDAO.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("Tên đăng nhập (Username) đã tồn tại.");
        }

        // 2. Băm mật khẩu và map dữ liệu
        String hashedPassWord = hashPassword(request.getPassword());
        User newUser = UserMapper.toEntity(request);
        newUser.setPasswordHash(hashedPassWord);

        // 3. Ghi xuống DB (DB nên có ràng buộc UNIQUE cho email/username)
        return this.userDAO.insertUser(newUser);
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
     * Xử lý đăng xuất (Xóa phiên làm việc và dọn rác Realtime).
     * @return true nếu đăng xuất và dọn dẹp thành công.
     */
    public boolean logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // 1. Lấy userId ra TRƯỚC KHI xóa token để có ID dọn dẹp Realtime
        String userId = userCache.get(token);

        if (userId != null) {
            // 2. Hủy thẻ từ (Token) khỏi RAM
            userCache.remove(token);

            // 3. Dọn dẹp: Rút người dùng khỏi tất cả các phòng đấu giá đang theo dõi
            // Nhớ import com.uet.BiddingApplication.Service.RealtimeBroadcastService;
            RealtimeBroadcastService.getInstance().unsubscribeFromAll(userId);

            return true;
        }

        return false;
    }

    /**
     * Thay đổi mật khẩu người dùng.
     * @return true nếu cập nhật cơ sở dữ liệu thành công.
     */
    public boolean changePassword(PasswordChangeRequestDTO request, String userId) {
        // 1. Tìm thông tin user hiện tại (Sử dụng biến instance this.userDAO cho clean)
        User user = this.userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Không tìm thấy thông tin người dùng.");
        }

        // 2. Validate nghiệp vụ: Mật khẩu cũ phải khớp
        if (!checkPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu cũ không chính xác.");
        }

        // 3. Băm mật khẩu mới, gọi DAO và trả thẳng kết quả boolean cho Router
        String hashedNewPassword = hashPassword(request.getNewPassword());
        return this.userDAO.changePassword(userId, hashedNewPassword);
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân.
     * @return true nếu cập nhật cơ sở dữ liệu thành công.
     */
    public boolean updateProfile(ProfileUpdateRequestDTO request, String userId) {
        // 1. Tìm thông tin user hiện tại
        User user = this.userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Không tìm thấy thông tin người dùng.");
        }

        // 2. Đổ dữ liệu mới từ request (DTO) vào thực thể (Entity)
        UserMapper.updateEntity(request, user);

        // 3. Ghi xuống Database và trả thẳng kết quả boolean cho Router
        return this.userDAO.updateProfile(user);
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