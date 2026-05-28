package com.uet.BiddingApplication.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import com.uet.BiddingApplication.DAO.Impl.*;
import com.uet.BiddingApplication.DTO.Request.SessionRegisterRequestDTO;
import com.uet.BiddingApplication.DTO.Request.SessionTargetRequestDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.AuctionRoomSyncDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.AutoBidSetting;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Model.SessionRegistration;
import com.uet.BiddingApplication.Utils.Mapper.AuctionSessionMapper;

public class BidderService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BidderService.class);

    private static volatile BidderService instance = null;

    private BidderService(){
    }

    public static BidderService getInstance() {
        if (instance == null) {
            synchronized (BidderService.class) {
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
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không được để trống.");
        }

        return SessionRegistrationDAO.getInstance().getRegisteredSessions(bidderId);
    }

    /**
     * Trả về lịch sử tất cả các lần tham gia trả giá của Bidder.
     */
    public List<BidderHistoryResponseDTO> getBidderHistory(String bidderId) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không được để trống.");
        }

        return BidDAO.getInstance().getBidderHistory(bidderId);
    }

    /**
     * Đăng ký tham gia một phiên đấu giá.
     * * @param request DTO chứa mã phiên cần đăng ký
     * @param bidderId ID của user đang thực hiện request (lấy từ Token)
     * @return true nếu đăng ký thành công
     */
    public boolean registerSession(SessionRegisterRequestDTO request, String bidderId) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không hợp lệ.");
        }
        if (request == null || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new BusinessException("Mã phiên đấu giá (Session ID) không được để trống.");
        }

        String sessionId = request.getSessionId();

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException("Phiên đấu giá không tồn tại trong hệ thống.");
        }

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException("Không thể đăng ký! Phiên đấu giá này hiện không ở trạng thái mở đăng ký.");
        }

        boolean isAlreadyRegistered = SessionRegistrationDAO.getInstance().checkRegistration(bidderId, sessionId);
        if (isAlreadyRegistered) {
            throw new BusinessException("Bạn đã đăng ký tham gia phiên đấu giá này từ trước rồi.");
        }

        SessionRegistration registration = AuctionSessionMapper.toEntity(request, bidderId);

        boolean isSuccess = SessionRegistrationDAO.getInstance().registerBidder(registration);

        if (isSuccess) {
            return true;
        } else {
            throw new BusinessException("Lỗi hệ thống: Không thể ghi nhận yêu cầu đăng ký của bạn lúc này.");
        }
    }


    /**
     * Hủy đăng ký tham gia một phiên đấu giá.
     *
     * @param request DTO chứa mã phiên cần hủy đăng ký
     * @param bidderId ID của user đang thực hiện request (lấy từ Token)
     * @return true nếu hủy thành công
     */
    public boolean cancelSessionRegistration(SessionRegisterRequestDTO request, String bidderId){
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng (Bidder ID) không hợp lệ.");
        }
        if (request == null || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new BusinessException("Mã phiên đấu giá (Session ID) không được để trống.");
        }

        String sessionId = request.getSessionId();

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException("Phiên đấu giá không tồn tại trong hệ thống.");
        }

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new BusinessException("Không thể đăng ký! Phiên đấu giá này hiện không ở trạng thái mở đăng ký.");
        }

        boolean isAlreadyRegistered = SessionRegistrationDAO.getInstance().checkRegistration(bidderId, sessionId);
        if (!isAlreadyRegistered) {
            throw new BusinessException("Bạn đã hủy đăng ký tham gia phiên đấu giá này từ trước rồi.");
        }

        boolean isSuccess = SessionRegistrationDAO.getInstance().deleteRegistration(bidderId, sessionId);

        if (isSuccess) {
            return true;
        } else {
            throw new BusinessException("Lỗi hệ thống: Không thể ghi nhận yêu cầu hủy đăng ký của bạn lúc này.");
        }
    }


    /**
     * Thoát khỏi phòng đấu giá (Leave Room), ngừng nhận sự kiện realtime.
     *
     * @param request DTO chứa ID của phiên đấu giá cần rời khỏi.
     * @param bidderId ID của user.
     */
    public void leaveSession(SessionTargetRequestDTO request, String bidderId) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng không hợp lệ.");
        }
        if (request == null || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new BusinessException("Mã phiên đấu giá (Session ID) không được để trống.");
        }

        String sessionId = request.getSessionId();
        RealtimeBroadcastService.getInstance().unsubscribe(sessionId, bidderId);
    }

    /**
     * Tham gia vào phòng đấu giá (Join Room) để nhận sự kiện realtime
     * và đồng bộ dữ liệu giao diện ban đầu.
     *
     * @param request DTO chứa ID của phiên đấu giá.
     * @param bidderId ID của user (từ token).
     * @return DTO chứa thông tin đồng bộ toàn diện của phòng đấu giá.
     */
    public AuctionRoomSyncDTO joinSession(SessionTargetRequestDTO request, String bidderId) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new BusinessException("Mã người dùng không hợp lệ.");
        }
        if (request == null || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new BusinessException("Mã phiên đấu giá không được để trống.");
        }

        String sessionId = request.getSessionId();

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException("Không tìm thấy phiên đấu giá này.");
        }

        if (session.getStatus() == SessionStatus.FINISHED || session.getStatus() == SessionStatus.CANCELED) {
            throw new BusinessException("Phiên đấu giá đã kết thúc hoặc bị hủy.");
        }


        String imageURL = null;
        String itemId = session.getItemId();
        Item item =null;
        if (itemId != null) {
            item = ItemDAO.getInstance().getItemById(itemId);
            if (item != null) {
                imageURL = item.getImageURL();
            }
        }

        List<BidHistoryDTO> sessionHistory = BidDAO.getInstance().getRecentBids(sessionId);

        String highestBidderName = null;
        if (sessionHistory != null && !sessionHistory.isEmpty()) {
            highestBidderName = sessionHistory.getFirst().getBidderName();
        }
        AutoBidSetting autoBidSetting= AutoBidSettingDAO.getInstance().getAutoBid(bidderId,sessionId);

        RealtimeBroadcastService.getInstance().subscribe(sessionId, bidderId);

        long endTime=session.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        BigDecimal currentPrice = session.getCurrentPrice()!=null?session.getCurrentPrice():session.getStartPrice();
        return new AuctionRoomSyncDTO(
                session.getId(),
                item.getName(),
                imageURL,
                item.getDescription(),
                currentPrice,
                session.getBidStep(),
                autoBidSetting,
                endTime-System.currentTimeMillis(),
                sessionHistory,
                highestBidderName
        );
    }
}