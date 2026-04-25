package com.uet.BiddingApplication.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.AdminActionRequestDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.RoleType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.Admin;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.ServerClass.AuctionServer;

/**
 * Lớp nghiệp vụ dành riêng cho chức năng của Quản trị viên (Admin).
 * Áp dụng mẫu thiết kế Singleton.
 */
public class AdminService {

    private static volatile AdminService instance = null;

    private AdminService(){
        // TODO: Khởi tạo instance của UserDAO và AuctionSessionDAO.
    }

    public static AdminService getInstance(){
        if (instance == null){
            synchronized (AdminService.class){
                if (instance == null){
                    instance = new AdminService();
                }
            }
        }
        return instance;
    }

    // Tách riêng logic sinh mã để đảm bảo Single Responsibility (SRP)
    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * Lấy toàn bộ danh sách người dùng.
     */
    public List<User> getAllUsers(){
        // TODO 1 (Dependencies): Lấy instance của UserDAO.
        // TODO 2 (Output): Trả về danh sách User từ hàm getAllUsers().
        return UserDAO.getInstance().getAllUsers();
    }

    /**
     * Lấy toàn bộ danh sách phiên đấu giá.
     */
    public List<AuctionSession> getAllSessions(){
        // TODO 1 (Dependencies): Lấy instance của AuctionSessionDAO.
        // TODO 2 (Output): Trả về danh sách phiên.
        return AuctionSessionDAO.getInstance().getAllSessions(true);
    }


    /**
     * Khóa tài khoản người dùng và ngắt kết nối lập tức.
     * Trả về true nếu thành công, ném BusinessException nếu thất bại để Router xử lý.
     */
    public boolean banUser(AdminActionRequestDTO request, String adminId) {
        // 1. Fail-Fast: Kiểm tra dữ liệu Request đầu vào
        if (request == null || request.getTargetId() == null || request.getOtpCode() == null) {
            throw new BusinessException("Dữ liệu yêu cầu không hợp lệ.");
        }

        // 2. Fail-Fast: Xác thực quyền Admin và lấy OTP độc nhất từ đối tượng Admin
        User currentUser = UserDAO.getInstance().findById(adminId);
        if (!(currentUser instanceof Admin admin)) {
            throw new BusinessException("Bạn không có quyền thực hiện hành động này.");
        }

        // So khớp với mã OTP độc nhất đã được gán sẵn cho Admin này
        if (!admin.getOtpSecretKey().equals(request.getOtpCode())) {
            throw new BusinessException("Mã OTP xác thực không chính xác.");
        }

        // 3. Fail-Fast & Bảo vệ nghiệp vụ: Kiểm tra người dùng mục tiêu
        User targetUser = UserDAO.getInstance().findById(request.getTargetId());
        if (targetUser == null) {
            throw new BusinessException("Người dùng cần khóa không tồn tại.");
        }

        // Nguyên tắc an toàn: Quản trị viên không được phép khóa Quản trị viên khác
        if (targetUser.getRole() == RoleType.ADMIN) {
            throw new BusinessException("Không thể khóa tài khoản của Quản trị viên khác.");
        }

        // 4. Thực hiện nghiệp vụ (Bước 1): Cập nhật trạng thái trong Database [cite: 1121]
        // Chuyển isActive thành false trước khi ngắt kết nối
        boolean isUpdated = UserDAO.getInstance().updateStatus(request.getTargetId(), false);
        if (!isUpdated) {
            throw new BusinessException("Lỗi hệ thống: Không thể cập nhật trạng thái người dùng.");
        }

        // 5. Thực hiện nghiệp vụ (Bước 2): Ngắt kết nối Socket (Side-effect) [cite: 970]
        // Sau khi lưu DB thành công mới "đá" người dùng ra khỏi mạng
        AuctionServer.getInstance().kickUser(request.getTargetId());

        return true; // Trả về true để xác nhận mọi bước đã hoàn tất thành công
    }

    /**
     * Hủy bỏ một phiên đấu giá khẩn cấp.
     * Trả về true nếu thành công, ném BusinessException nếu thất bại để Router xử lý.
     */
    public boolean cancelSession(AdminActionRequestDTO request, String adminId) {
        // 1. Fail-Fast: Kiểm tra dữ liệu Request đầu vào
        if (request == null || request.getTargetId() == null || request.getOtpCode() == null) {
            throw new BusinessException("Dữ liệu yêu cầu hủy phiên không hợp lệ.");
        }

        String sessionId = request.getTargetId();

        // 2. Fail-Fast: Xác thực quyền Admin và mã OTP độc nhất
        User currentUser = UserDAO.getInstance().findById(adminId);
        if (!(currentUser instanceof Admin admin)) {
            throw new BusinessException("Bạn không có quyền thực hiện hành động quản trị này.");
        }

        if (!admin.getOtpSecretKey().equals(request.getOtpCode())) {
            throw new BusinessException("Mã OTP xác thực không chính xác.");
        }

        // 3. Fail-Fast & Bảo vệ toàn vẹn nghiệp vụ: Kiểm tra trạng thái phiên đấu giá
        AuctionSession targetSession = AuctionSessionDAO.getInstance().getSessionById(sessionId);
        if (targetSession == null) {
            throw new BusinessException("Phiên đấu giá mục tiêu không tồn tại.");
        }

        // Nguyên tắc an toàn: Không thể hủy một phiên đã kết thúc hợp lệ, đã thanh toán, hoặc đã bị hủy từ trước
        // (Giả định Enum SessionStatus bao gồm: OPEN, RUNNING, FINISHED, PAID, CANCELED)
        if (targetSession.getStatus() == SessionStatus.FINISHED ||
                targetSession.getStatus() == SessionStatus.CANCELED) {
            throw new BusinessException("Không thể hủy phiên đấu giá vì phiên này đã kết thúc hoặc đã bị hủy trước đó.");
        }

        // 4. Thực hiện nghiệp vụ (Bước 1): Cập nhật trạng thái Database TRƯỚC
        boolean isUpdated = AuctionSessionDAO.getInstance().updateStatus(sessionId, SessionStatus.CANCELED);
        if (!isUpdated) {
            throw new BusinessException("Lỗi hệ thống: Không thể cập nhật trạng thái hủy phiên vào cơ sở dữ liệu.");
        }

        // 5. Thực hiện nghiệp vụ (Bước 2): Tách biệt Network & Cache (Side-effects)
        // a. Xóa phiên khỏi RAM để không ai có thể tìm thấy trên trang chủ nữa
        SearchCacheManager.getInstance().removeSession(sessionId);

        // b. Phát thanh thông báo đóng phòng khẩn cấp cho tất cả Bidder đang xem phiên này
        ResponsePacket<String> cancelNotification = new ResponsePacket<>(
                ActionType.REALTIME_SESSION_END, // Hoặc một ActionType cụ thể cho việc Hủy phiên
                200,
                "Phiên đấu giá đã bị Quản trị viên hủy bỏ khẩn cấp. Lý do: " + request.getActionReason(),
                sessionId
        );
        RealtimeBroadcastService.getInstance().broadcast(sessionId, cancelNotification);

        return true; // Hoàn tất an toàn
    }
}