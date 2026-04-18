package com.uet.BiddingApplication.Service;

import com.uet.BiddingApplication.CoreService.SearchCacheManager;
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

/**
 * Lớp quản lý nghiệp vụ tạo mới và tái đăng bán sản phẩm.
 * Áp dụng mẫu thiết kế Singleton.
 */
public class ItemManagementService {

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
     */
    public void createItemAndOpenSession(ItemCreateDTO request, String sellerId){
        // TODO 1 (Input): Nhận DTO từ Seller chứa thông tin vật phẩm và phiên.
        // TODO 2 (Processing): Khởi tạo Entity Item và gọi itemDAO.insertItem().
        // TODO 3 (Processing): Khởi tạo Entity AuctionSession và gọi sessionDAO.insertSession().
        // TODO 4 (Side-effect): Gọi SearchCacheManager.getInstance().addSessionAndItem(...) để nạp dữ liệu nóng lên RAM.
        String imageURL;

        try {
            imageURL = StorageService.getInstance().uploadImage(request.getImageBytes(), request.getImageExtension());
        } catch (Exception e) {
            throw new BusinessException("Lỗi upload Ảnh!");
        }

        Item newItem = ItemMapper.toEntity(request, sellerId, imageURL);
        itemDAO.insertItem(newItem);

        AuctionSession newSession = AuctionSessionMapper.toEntity(request, newItem.getId());
        sessionDAO.insertSession(newSession);

        SearchCacheManager.getInstance().addSessionAndItem(newSession, newItem);
    }

    /**
     * Đăng bán lại sản phẩm không ai mua (Ế).
     */
    public void relistUnsoldItem(RelistRequestDTO request){
        // TODO 1 (Input): Nhận ID của sản phẩm cần bán lại.
        // TODO 2 (Dependencies): Lấy thông tin Item cũ từ ItemDAO.
        // TODO 3 (Processing): Tạo mới Entity AuctionSession liên kết với itemId cũ.
        // TODO 4 (Output): Gọi sessionDAO.insertSession(...) để lưu phiên mới.

        String itemId = request.getItemId();
        Item oldItem = itemDAO.getItemById(itemId);

        if (oldItem == null){
            throw new BusinessException("Vật phẩm không tồn tại");
        }

        if (itemId.equals(oldItem.getId())){
            throw new BusinessException("Bạn không có quyền thao tác với vật phẩm này.");
        }

        AuctionSession oldSession = sessionDAO.getSessionByItemId(itemId);

        if (oldSession.getStatus() != SessionStatus.FINISHED && oldSession.getStatus() != SessionStatus.CANCELED){
            throw new BusinessException("Phiên đấu giá này chưa kết thúc hoặc chưa bị hủy.");
        }

        AuctionSession newSession = AuctionSessionMapper.toEntity(request);
        sessionDAO.insertSession(newSession);
    }
}