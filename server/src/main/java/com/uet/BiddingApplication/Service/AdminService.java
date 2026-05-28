package com.uet.BiddingApplication.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
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
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Response.UserProfileDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.UserMapper;
import com.uet.BiddingApplication.Utils.Mapper.AuctionViewMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * Lớp nghiệp vụ dành riêng cho chức năng của Quản trị viên (Admin).
 * Áp dụng mẫu thiết kế Singleton.
 */
public class AdminService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminService.class);

    private static volatile AdminService instance = null;

    private AdminService(){
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

    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * Lấy toàn bộ danh sách người dùng dưới dạng DTO.
     */
    public List<UserProfileDTO> getAllUsers(){
        List<User> users = UserDAO.getInstance().getAllUsers();
        if (users == null) {
            return java.util.Collections.emptyList();
        }
        return users.stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy toàn bộ danh sách phiên đấu giá dưới dạng DTO.
     */
    public List<AuctionCardDTO> getAllSessions(){
        List<AuctionSession> sessions = AuctionSessionDAO.getInstance().getAllSessions(true);
        if (sessions == null || sessions.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> itemIds = sessions.stream()
                .map(AuctionSession::getItemId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Item> items = ItemDAO.getInstance().getItemsByIds(itemIds);
        Map<String, Item> itemMap = new HashMap<>();
        if (items != null) {
            for (Item item : items) {
                if (item != null && item.getId() != null) {
                    itemMap.put(item.getId(), item);
                }
            }
        }

        List<AuctionSession> validSessions = new ArrayList<>();
        for (AuctionSession session : sessions) {
            if (session != null && itemMap.containsKey(session.getItemId())) {
                validSessions.add(session);
            }
        }

        return AuctionViewMapper.toCardDTOList(validSessions, itemMap);
    }


    /**
     * Khóa tài khoản người dùng và ngắt kết nối lập tức.
     * Trả về true nếu thành công, ném BusinessException nếu thất bại để Router xử lý.
     */
    public boolean banUser(AdminActionRequestDTO request, String adminId) {
        if (request == null || request.getTargetId() == null || request.getOtpCode() == null) {
            throw new BusinessException("Dữ liệu yêu cầu không hợp lệ.");
        }

        User currentUser = UserDAO.getInstance().findById(adminId);
        if (!(currentUser instanceof Admin admin)) {
            throw new BusinessException("Bạn không có quyền thực hiện hành động này.");
        }

        if (!admin.getOtpSecretKey().equals(request.getOtpCode())) {
            throw new BusinessException("Mã OTP xác thực không chính xác.");
        }

        User targetUser = UserDAO.getInstance().findById(request.getTargetId());
        if (targetUser == null) {
            throw new BusinessException("Người dùng cần khóa không tồn tại.");
        }

        if (targetUser.getRole() == RoleType.ADMIN) {
            throw new BusinessException("Không thể khóa tài khoản của Quản trị viên khác.");
        }

        boolean isUpdated = UserDAO.getInstance().updateStatus(request.getTargetId(), false);
        if (!isUpdated) {
            throw new BusinessException("Lỗi hệ thống: Không thể cập nhật trạng thái người dùng.");
        }
        AutoBidManager.getInstance().removeAutoBidsForBannedUser(request.getTargetId());

        AuctionServer.getInstance().kickUser(request.getTargetId());

        return true;
    }

    /**
     * Hủy bỏ một phiên đấu giá khẩn cấp.
     * Trả về true nếu thành công, ném BusinessException nếu thất bại để Router xử lý.
     */
    public boolean cancelSession(AdminActionRequestDTO request, String adminId) {
        if (request == null || request.getTargetId() == null || request.getOtpCode() == null) {
            throw new BusinessException("Dữ liệu yêu cầu hủy phiên không hợp lệ.");
        }

        String sessionId = request.getTargetId();

        User currentUser = UserDAO.getInstance().findById(adminId);
        if (!(currentUser instanceof Admin admin)) {
            throw new BusinessException("Bạn không có quyền thực hiện hành động quản trị này.");
        }

        if (!admin.getOtpSecretKey().equals(request.getOtpCode())) {
            throw new BusinessException("Mã OTP xác thực không chính xác.");
        }

        AuctionSession targetSession = AuctionSessionDAO.getInstance().getSessionById(sessionId);
        if (targetSession == null) {
            throw new BusinessException("Phiên đấu giá mục tiêu không tồn tại.");
        }

        if (targetSession.getStatus() == SessionStatus.FINISHED ||
                targetSession.getStatus() == SessionStatus.CANCELED) {
            throw new BusinessException("Không thể hủy phiên đấu giá vì phiên này đã kết thúc hoặc đã bị hủy trước đó.");
        }

        boolean isUpdated = AuctionSessionDAO.getInstance().updateStatus(sessionId, SessionStatus.CANCELED);
        if (!isUpdated) {
            throw new BusinessException("Lỗi hệ thống: Không thể cập nhật trạng thái hủy phiên vào cơ sở dữ liệu.");
        }

        SearchCacheManager.getInstance().removeSession(sessionId);
        SessionStartScheduler.getInstance().cancelSchedule(sessionId);

        ResponsePacket<String> cancelNotification = new ResponsePacket<>(
                ActionType.REALTIME_SESSION_END,
                200,
                "Phiên đấu giá đã bị Quản trị viên hủy bỏ khẩn cấp. Lý do: " + request.getActionReason(),
                sessionId
        );
        RealtimeBroadcastService.getInstance().broadcast(sessionId, cancelNotification);

        return true;
    }
}