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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private static volatile AuthService instance;

    private UserDAO userDAO;

    private ConcurrentHashMap<String, String> userCache;

    private AuthService() {
        this.userDAO = UserDAO.getInstance();
        this.userCache = new ConcurrentHashMap<>();
    }

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }


    /**
     * Xử lý đăng nhập, tạo token và kiểm tra đăng nhập trùng lặp.
     */
    public AuthResponseDTO login(AuthRequestDTO request) {
        User user = this.userDAO.findByUsername(request.getUsername());

        if (user == null || !checkPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Tài khoản không tồn tại hoặc sai mật khẩu.");
        }

        if (!user.isActive()) {
            throw new BusinessException("Tài khoản của bạn đã bị Admin khóa.");
        }

        String userId = String.valueOf(user.getId());

        ClientConnectionHandler oldHandler = AuctionServer.getInstance().getClientHandler(userId);
        if (oldHandler != null) {
            oldHandler.kickOut("Tài khoản đã bị đăng nhập ở nơi khác !");
        }

        String token = UUID.randomUUID().toString();
        userCache.put(token, userId);
        userDAO.updateSessionToken(userId, token);

        UserProfileDTO userProfile = UserMapper.toDto(user);
        return new AuthResponseDTO(token, userProfile);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public boolean register(RegisterRequestDTO request) {
        if (this.userDAO.findByEmail(request.getEmail()) != null) {
            throw new BusinessException("Email này đã được đăng ký.");
        }
        if (this.userDAO.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("Tên đăng nhập (Username) đã tồn tại.");
        }

        String hashedPassWord = hashPassword(request.getPassword());
        User newUser = UserMapper.toEntity(request);
        newUser.setPasswordHash(hashedPassWord);

        if (!this.userDAO.insertUser(newUser)){
            throw new BusinessException("Lỗi DAO không đăng ký được");
        }
        return true;
    }

    /**
     * Kiểm tra tính hợp lệ của token và trả về userId tương ứng.
     */
    public String validateToken(String token) {
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

        String userId = userCache.get(token);

        if (userId != null) {
            userCache.remove(token);
            userDAO.updateSessionToken(userId, null);

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
        User user = this.userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Không tìm thấy thông tin người dùng.");
        }

        if (!checkPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu cũ không chính xác.");
        }

        String hashedNewPassword = hashPassword(request.getNewPassword());
        if (!this.userDAO.changePassword(userId, hashedNewPassword)){
            throw new BusinessException("Lỗi DAO không thay đổi được mật khẩu");
        }
        return true;
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân.
     * @return true nếu cập nhật cơ sở dữ liệu thành công.
     */
    public UserProfileDTO updateProfile(ProfileUpdateRequestDTO request, String userId) {
        User user = this.userDAO.findById(userId);
        if (user == null) {
            throw new BusinessException("Không tìm thấy thông tin người dùng.");
        }

        UserMapper.updateEntity(request, user);

        if (!this.userDAO.updateProfile(user)){
            throw new BusinessException("Lỗi DAO không update được tài khoản");
        }
        return UserMapper.toDto(user);
    }
    public void reconnect(String userId,String oldToken) {
        if (oldToken == null || oldToken.trim().isEmpty()) {
            throw new BusinessException("Token không được để trống.");
        }

        String token=userDAO.getTokenById(userId);
        if (!token.equals(oldToken)) {
            throw new BusinessException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }

        userCache.put(token, userId);
    }

    private boolean checkPassword(String plain, String hashed) {
        return BCrypt.checkpw(plain, hashed);
    }

    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }
}