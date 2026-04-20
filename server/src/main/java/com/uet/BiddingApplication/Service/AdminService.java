package com.uet.BiddingApplication.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.UserDAO;
import com.uet.BiddingApplication.DTO.Request.AdminActionRequestDTO;
import com.uet.BiddingApplication.Model.User;
import com.uet.BiddingApplication.Model.AuctionSession;

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
     * Sinh và gửi mã OTP cho Admin khi thực hiện các hành động nhạy cảm.
     */
    public void requestOtp(String adminId){
        // TODO 1 (Processing): Sinh mã ngẫu nhiên 6 chữ số.
        // TODO 2 (Side-effect): Lưu OTP vào Cache hoặc DB với thời gian hết hạn.
        // TODO 3 (Output): In ra console để test (hoặc tích hợp gửi Email/SMS sau này).

        // 1. Sinh mã OTP 6 số
        String otpCode = generateOTP();

        // 2. Tính toán thời gian hết hạn (ví dụ: 5 phút kể từ hiện tại)
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(5);

        // 4. Giả lập gửi OTP bằng cách in ra console để test
        System.out.println("\n=== THÔNG BÁO HỆ THỐNG ===");
        System.out.println("Yêu cầu cấp OTP từ Admin ID: " + adminId);
        System.out.println("Mã OTP của bạn là: " + otpCode);
        System.out.println("Mã có hiệu lực đến: " + expirationTime);
        System.out.println("==========================\n");
    }

    /**
     * Khóa tài khoản người dùng và ngắt kết nối lập tức.
     */
    public void banUser(AdminActionRequestDTO request){
        // TODO 1 (Input): Kiểm tra request.getOtpCode() có trùng khớp không.
        // TODO 2 (Processing): Gọi UserDAO cập nhật trạng thái isActive = false.
        // TODO 3 (Side-effect/Quan trọng): Gọi AuctionServer.getInstance().kickUser(request.getTargetId()) để đóng Socket.
    }

    /**
     * Hủy bỏ một phiên đấu giá khẩn cấp.
     */
    public void cancelSession(AdminActionRequestDTO request){
        // TODO 1 (Input): Kiểm tra mã OTP.
        // TODO 2 (Processing): Gọi AuctionSessionDAO để update trạng thái (status) thành CANCELED.
    }
}