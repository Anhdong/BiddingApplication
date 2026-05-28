package com.uet.BiddingApplication.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
import com.uet.BiddingApplication.CoreService.SessionStartScheduler;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.ItemDAO;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.DTO.Request.ItemUpdateRequestDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;
import com.uet.BiddingApplication.Utils.Mapper.AuctionSessionMapper;
import com.uet.BiddingApplication.Utils.Mapper.ItemMapper;
import com.uet.BiddingApplication.Utils.StorageService;

/**
 * Lớp nghiệp vụ dành cho các chức năng của Người bán (Seller).
 * Áp dụng mẫu thiết kế Singleton.
 */
public class SellerService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellerService.class);

    private static volatile SellerService instance = null;

    private SellerService(){
    }

    public static SellerService getInstance(){
        if (instance == null){
            synchronized (SellerService.class){
                if (instance == null){
                    instance = new SellerService();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy thống kê lịch sử các mặt hàng đã kết thúc của Seller.
     */
    public List<SellerHistoryResponseDTO> getSellerHistory(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new BusinessException("Mã định danh người bán (Seller ID) không được để trống.");
        }

        return AuctionSessionDAO.getInstance().getSellerHistory(sellerId);
    }

    /**
     * Cập nhật thông tin vật phẩm.
     * Hợp lệ khi phiên chưa được tạo (null) hoặc đang ở trạng thái chờ/OPEN.
     */
    public boolean updateItem(ItemUpdateRequestDTO request) {
        String itemId = request.getItemId();

        Item item = ItemDAO.getInstance().getItemById(itemId);
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại trong hệ thống.");
        }

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        if (session != null && (session.getStatus() == SessionStatus.RUNNING ||
                session.getStatus() == SessionStatus.FINISHED)) {
            throw new BusinessException("Không thể cập nhật vật phẩm khi phiên đấu giá đang " + session.getStatus().name() + ".");
        }

        String newImageURL = item.getImageURL();
        boolean hasNewImage = false;

        if (request.getImageBytes() != null && request.getImageBytes().length > 0) {
            try {
                newImageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());
                hasNewImage = true;
            } catch (Exception e) {
                log.error("Lỗi upload ảnh mới cho itemId: " + itemId, e);
                throw new BusinessException("Lỗi trong quá trình tải ảnh mới lên hệ thống lưu trữ.");
            }
        }

        Item updateItem = ItemMapper.toEntity(request, newImageURL);

        if (session != null) {
            if (request.getStartTime() != null) session.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) session.setEndTime(request.getEndTime());

            if (request.getStartPrice() != null) {
                if (request.getStartPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Giá khởi điểm phải lớn hơn 0.");
                }
                session.setStartPrice(request.getStartPrice());
            }

            if (request.getBidStep() != null) {
                if (request.getBidStep().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Bước giá phải lớn hơn 0.");
                }
                session.setBidStep(request.getBidStep());
            }
        }

        boolean isItemUpdated = ItemDAO.getInstance().updateItem(updateItem);
        boolean isSessionUpdated = true;

        if (isItemUpdated && session != null) {
            isSessionUpdated = AuctionSessionDAO.getInstance().updateSession(session);
        }

        System.out.println(isItemUpdated + " " + isSessionUpdated);
        if (isItemUpdated && isSessionUpdated) {
            SearchCacheManager.getInstance().updateItem(itemId, updateItem);

            if (session != null) {
                SearchCacheManager.getInstance().updateSession(session.getId(), session);
                SessionStartScheduler.getInstance().cancelSchedule(session.getId());
                SessionStartScheduler.getInstance().scheduleStart(session.getId(),session.getStartTime());
            }

            if (hasNewImage && item.getImageURL() != null && !item.getImageURL().isEmpty()) {
                try {
                    StorageService.getInstance().deleteImage(item.getImageURL());
                } catch (Exception e) {
                    log.warn("[Warning] Lỗi khi dọn dẹp ảnh cũ trên Storage cho itemId: " + itemId, e);
                }
            }
            return true;
        } else {
            if (hasNewImage) {
                try {
                    StorageService.getInstance().deleteImage(newImageURL);
                } catch (Exception e) {
                    log.error("[Error] Không thể rollback ảnh mới trên Storage khi DB update lỗi.", e);
                }
            }
            throw new BusinessException("Lỗi hệ thống: Không thể đồng bộ dữ liệu cập nhật xuống cơ sở dữ liệu.");
        }
    }

    /**
     * Xóa vật phẩm khỏi hệ thống.
     * Áp dụng logic kiểm tra phiên và dọn dẹp tài nguyên lưu trữ.
     */
    public boolean deleteItem(String itemId) {
        Item item = ItemDAO.getInstance().getItemById(itemId);
        if (item == null) {
            throw new BusinessException("Vật phẩm không tồn tại hoặc đã bị xóa trước đó.");
        }

        AuctionSession session = AuctionSessionDAO.getInstance().getSessionByItemId(itemId);

        if (session != null) {
            SessionStatus status = session.getStatus();
            if (status == SessionStatus.RUNNING || status == SessionStatus.FINISHED) {
                throw new BusinessException("Không thể xóa vật phẩm khi phiên đấu giá đang " + status.name() + ".");
            }
        }

        if (item.getImageURL() != null && !item.getImageURL().isEmpty()) {
            try {
                StorageService.getInstance().deleteImage(item.getImageURL());
            } catch (Exception e) {
                log.error("[Warning] Không thể dọn dẹp ảnh cũ trên Storage cho itemId: " + itemId);
            }
        }

        boolean isDeleted = ItemDAO.getInstance().deleteItem(itemId);

        if (isDeleted) {
            SearchCacheManager.getInstance().removeItem(itemId);
            if (session != null) {
                SearchCacheManager.getInstance().removeSession(session.getId());
                SessionStartScheduler.getInstance().cancelSchedule(session.getId());
            }
            return true;
        } else {
            throw new BusinessException("Lỗi hệ thống: Không thể xóa vật phẩm khỏi cơ sở dữ liệu.");
        }
    }
    /**
     * Lấy danh sách các sản phẩm mà Seller đã đăng tải để hiển thị lên kho quản lý.
     */
    public List<AuctionCardDTO> getItemsBySellerId(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new BusinessException("Mã định danh người bán (Seller ID) không hợp lệ.");
        }

        List<AuctionCardDTO> items = ItemDAO.getInstance().getSellerItems(sellerId);

        return items != null ? items : new ArrayList<>();
    }
}