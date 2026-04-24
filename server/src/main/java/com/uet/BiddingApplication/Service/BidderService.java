package com.uet.BiddingApplication.Service;

import java.util.List;

import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Impl.SessionRegistrationDAO;
import com.uet.BiddingApplication.DTO.Request.SessionRegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.SessionRegistration;
import com.uet.BiddingApplication.Utils.Mapper.AuctionSessionMapper;

public class BidderService {

    // Khởi tạo Singleton
    private static volatile BidderService instance = null;

    // Constructor private để ngăn khởi tạo từ bên ngoài
    private BidderService(){
        // Tài liệu đặc tả không yêu cầu thuộc tính cho class này.
        // Bạn sẽ gọi trực tiếp các Singleton DAO bên trong các phương thức.
    }

    // Lấy instance duy nhất của BidderService
    public static BidderService getInstance() {
        // Kiểm tra lần 1: Bỏ qua khóa nếu đối tượng đã được tạo
        if (instance == null) {
            // Chỉ khóa class tại thời điểm khởi tạo lần đầu tiên
            synchronized (BidderService.class) {
                // Kiểm tra lần 2: Tránh trường hợp 2 luồng cùng lọt qua lần kiểm tra 1
                if (instance == null) {
                    instance = new BidderService();
                }
            }
        }
        return instance;
    }

    /**
     * Trả về danh sách các phiên mà Bidder này đã đăng ký tham gia trước.
     */
    public List<AuctionCardDTO> getRegisteredSessions(String bidderId) {
        // 1. Kiểm tra tính hợp lệ của đầu vào (Defensive Programming)
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không được để trống.");
        }

        // 2. Gọi DAO lấy trực tiếp danh sách DTO đã được JOIN sẵn từ Database
        return SessionRegistrationDAO.getInstance().getRegisteredSessions(bidderId);
    }

    /**
     * Trả về lịch sử tất cả các lần tham gia trả giá của Bidder.
     */
    public List<BidderHistoryResponseDTO> getBidderHistory(String bidderId) {
        // 1. Kiểm tra tính hợp lệ của đầu vào (Defensive Programming)
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không được để trống.");
        }

        // 2. Trích xuất dữ liệu
        // DAO đã xử lý toàn bộ logic gom nhóm (GROUP BY) và mapping sang DTO,
        // Service chỉ việc lấy kết quả và trả về cho RequestRouter.
        return BidDAO.getInstance().getBidderHistory(bidderId);
    }

    /**
     * Đăng ký tham gia một phiên đấu giá.
     * * @param request DTO chứa mã phiên cần đăng ký
     * @param bidderId ID của user đang thực hiện request (lấy từ Token)
     * @return true nếu đăng ký thành công
     */
    public boolean registerSession(SessionRegisterRequestDTO request, String bidderId) {
        // 1. Kiểm tra tính hợp lệ của đầu vào (Fail-fast)
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không hợp lệ.");
        }
        if (request == null || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new BusinessException("Mã phiên đấu giá (Session ID) không được để trống.");
        }

        String sessionId = request.getSessionId();

        // 2. Ràng buộc nghiệp vụ: Kiểm tra trạng thái phiên đấu giá
        AuctionSession session = AuctionSessionDAO.getInstance().getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException("Phiên đấu giá không tồn tại trong hệ thống.");
        }

        // Theo luồng logic: OPEN (Đăng ký) -> RUNNING (Đấu giá) -> FINISHED
        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException("Không thể đăng ký! Phiên đấu giá này hiện không ở trạng thái mở đăng ký.");
        }

        // 3. Ràng buộc nghiệp vụ: Chống đăng ký trùng lặp
        boolean isAlreadyRegistered = SessionRegistrationDAO.getInstance().checkRegistration(bidderId, sessionId);
        if (isAlreadyRegistered) {
            throw new BusinessException("Bạn đã đăng ký tham gia phiên đấu giá này từ trước rồi.");
        }

        // 4. Map dữ liệu sang Entity để lưu
        SessionRegistration registration = AuctionSessionMapper.toEntity(request, bidderId);

        // 5. Ghi xuống Cơ sở dữ liệu
        boolean isSuccess = SessionRegistrationDAO.getInstance().registerBidder(registration);

        if (isSuccess) {
            return true;
        } else {
            // Lỗi này xảy ra khi Database từ chối (VD: lỗi khóa ngoại, đứt kết nối...)
            throw new BusinessException("Lỗi hệ thống: Không thể ghi nhận yêu cầu đăng ký của bạn lúc này.");
        }
    }
}