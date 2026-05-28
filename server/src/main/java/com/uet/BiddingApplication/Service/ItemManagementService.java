package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.DTO.Request.RelistRequestDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.AuctionSessionMapper;
import com.uet.BiddingApplication.Utils.Mapper.ItemMapper;
import com.uet.BiddingApplication.Utils.StorageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp quản lý nghiệp vụ tạo mới và tái đăng bán sản phẩm.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class ItemManagementService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ItemManagementService.class);

    private ItemDAO itemDAO;
    private AuctionSessionDAO sessionDAO;

    private static volatile ItemManagementService instance = null;

    private ItemManagementService(){
        this.itemDAO = ItemDAO.getInstance();
        this.sessionDAO = AuctionSessionDAO.getInstance();
    }

    public static ItemManagementService getInstance(){
        if (instance == null){
            synchronized (ItemManagementService.class) {
                if (instance == null) {
                    instance = new ItemManagementService();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo sản phẩm mới và mở phiên đấu giá cho sản phẩm đó.
     * @return true nếu mọi bước thực hiện thành công, ném Exception nếu có lỗi nghiệp vụ.
     */
    public boolean createItemAndOpenSession(ItemCreateDTO request, String sellerId) {
        if (request == null || sellerId == null) {
            throw new BusinessException("Dữ liệu yêu cầu không hợp lệ.");
        }

        if (request.getBidStep() == null || request.getBidStep().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Bước giá phải lớn hơn 0.");
        }

        String imageURL=null;
        if(request.getImageBytes()!=null) {
            try {
                imageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());
            } catch (Exception e) {
                throw new BusinessException("Không thể tải lên hình ảnh sản phẩm. Vui lòng thử lại.");
            }
        }

        Item newItem = ItemMapper.toEntity(request, sellerId, imageURL);

        if (!itemDAO.insertItem(newItem)) {
            throw new BusinessException("Lỗi hệ thống: Không thể khởi tạo thông tin vật phẩm.");
        }

        AuctionSession newSession = AuctionSessionMapper.toEntity(request, newItem.getId(), sellerId);
        if (!sessionDAO.insertSession(newSession)) {
            throw new BusinessException("Lỗi hệ thống: Không thể mở phiên đấu giá cho sản phẩm này.");
        }

        SearchCacheManager.getInstance().addSessionAndItem(newSession, newItem);

        SessionStartScheduler.getInstance().scheduleStart(newSession.getId(), newSession.getStartTime());

        return true;
    }

    /**
     * Cập nhật thông tin phiên đấu giá (nếu đang OPEN)
     * hoặc mở phiên đấu giá mới/đăng bán lại (nếu đã FINISHED/CANCELED).
     * @return true nếu thao tác thành công.
     */
    public boolean relistUnsoldItem(RelistRequestDTO request, String sellerId) {
        if (request == null || request.getItemId() == null || request.getSessionId() == null) {
            throw new BusinessException("Dữ liệu yêu cầu không hợp lệ.");
        }

        if (request.getNewStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Thời gian bắt đầu không thể diễn ra trong quá khứ.");
        }

        if (request.getNewStartTime().isAfter(request.getNewEndTime())) {
            throw new BusinessException("Thời gian bắt đầu không thể sau thời gian kết thúc.");
        }

        if (request.getNewStartPrice() == null || request.getNewStartPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá khởi điểm phải lớn hơn 0.");
        }

        if (request.getNewBidStep() == null || request.getNewBidStep().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Bước giá mới phải lớn hơn 0.");
        }

        Item item = itemDAO.getItemById(request.getItemId());
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại.");
        }

        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessException("Bạn không có quyền thao tác với vật phẩm này.");
        }

        AuctionSession oldSession = sessionDAO.getSessionById(request.getSessionId());
        if (oldSession == null) {
            throw new BusinessException("Phiên đấu giá không tồn tại.");
        }

        SessionStatus currentStatus = oldSession.getStatus();

        if (currentStatus == SessionStatus.OPEN) {
            oldSession.setStartPrice(request.getNewStartPrice());
            oldSession.setBidStep(request.getNewBidStep());
            oldSession.setStartTime(request.getNewStartTime());
            oldSession.setEndTime(request.getNewEndTime());

            if (!sessionDAO.updateSession(oldSession)) {
                throw new BusinessException("Lỗi hệ thống: Không thể cập nhật thông tin phiên đấu giá.");
            }

            SearchCacheManager.getInstance().addSessionAndItem(oldSession, item);
            SessionStartScheduler.getInstance().scheduleStart(oldSession.getId(), oldSession.getStartTime());
            return true;

        } else if (currentStatus == SessionStatus.FINISHED || currentStatus == SessionStatus.CANCELED) {
            AuctionSession newSession = AuctionSessionMapper.toEntity(request, sellerId);
            newSession.setItemId(item.getId());

            if (!sessionDAO.insertSession(newSession)) {
                throw new BusinessException("Lỗi hệ thống: Không thể mở lại phiên đấu giá mới.");
            }

            SearchCacheManager.getInstance().addSessionAndItem(newSession, item);
            SessionStartScheduler.getInstance().scheduleStart(newSession.getId(), newSession.getStartTime());
            return true;

        } else {
            throw new BusinessException("Không thể chỉnh sửa hoặc đăng lại khi phiên đấu giá đang diễn ra.");
        }
    }
}