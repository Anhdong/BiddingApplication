package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.CoreService.InMemoryBidServiceImpl;
import com.uet.BiddingApplication.CoreService.ItemSearchService;
import com.uet.BiddingApplication.Enum.Category;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;

import com.uet.BiddingApplication.DTO.Request.*;
import com.uet.BiddingApplication.DTO.Response.*;
import com.uet.BiddingApplication.Model.AutoBidSetting;

// Import toàn bộ các Service
import com.uet.BiddingApplication.Service.AuthService;
import com.uet.BiddingApplication.Service.AdminService;
import com.uet.BiddingApplication.Service.BidderService;
import com.uet.BiddingApplication.Service.ItemManagementService;
import com.uet.BiddingApplication.Service.SellerService;
import com.uet.BiddingApplication.Service.RealtimeBroadcastService;
import com.uet.BiddingApplication.Service.AutoBidManager;
import com.uet.BiddingApplication.Service.AuctionService;
import com.uet.BiddingApplication.Utils.Mapper.AutoBidMapper;
import com.uet.BiddingApplication.Exception.BusinessException;

import java.util.EnumMap;
import java.util.Map;

/**
 * File này đóng vai trò là bảng điều phối logic (Router) phía Server.
 */
public class RouteRegistry {

    public interface CommandHandler {
        ResponsePacket<?> handle(RequestPacket<?> request) throws Exception;
    }

    private static final Map<ActionType, CommandHandler> registry = new EnumMap<>(ActionType.class);

    static {
        // ==============================================================================
        // 1. NHÓM XÁC THỰC & CÁ NHÂN (AuthService)
        // ==============================================================================

        registry.put(ActionType.LOGIN, req -> {
            AuthRequestDTO dto = (AuthRequestDTO) req.getPayload();

            AuthResponseDTO result = AuthService.getInstance().login(dto);
            return new ResponsePacket<>(ActionType.LOGIN, 200, "Đăng nhập thành công", result);
        });

        registry.put(ActionType.REGISTER, req -> {
            RegisterRequestDTO dto = (RegisterRequestDTO) req.getPayload();
            AuthService.getInstance().register(dto);
            return new ResponsePacket<>(ActionType.REGISTER, 200, "Đăng ký thành công", null);
        });

        registry.put(ActionType.LOGOUT, req -> {
            String userId = req.getUserId();
            AuthService.getInstance().logout(req.getToken()); // Lưu ý: logout trong AuthService nhận token
            AuctionServer.getInstance().unregisterClient(userId);

            return new ResponsePacket<>(ActionType.LOGOUT, 200, "Đã đăng xuất", null);
        });

        registry.put(ActionType.CHANGE_PASSWORD, req -> {
            PasswordChangeRequestDTO dto = (PasswordChangeRequestDTO) req.getPayload();
            AuthService.getInstance().changePassword(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.CHANGE_PASSWORD, 200, "Đổi mật khẩu thành công", null);
        });

        registry.put(ActionType.UPDATE_PROFILE, req -> {
            ProfileUpdateRequestDTO dto = (ProfileUpdateRequestDTO) req.getPayload();
            AuthService.getInstance().updateProfile(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.UPDATE_PROFILE, 200, "Cập nhật thông tin thành công", null);
        });

        // ==============================================================================
        // 2. NHÓM QUẢN TRỊ VIÊN (AdminService)
        // ==============================================================================

        registry.put(ActionType.GET_ALL_USERS, req -> {
            Object result = AdminService.getInstance().getAllUsers();
            return new ResponsePacket<>(ActionType.GET_ALL_USERS, 200, "Lấy danh sách người dùng thành công", result);
        });

        registry.put(ActionType.GET_ALL_SESSIONS, req -> {
            Object result = AdminService.getInstance().getAllSessions();
            return new ResponsePacket<>(ActionType.GET_ALL_SESSIONS, 200, "Lấy danh sách phiên đấu giá thành công", result);
        });

        registry.put(ActionType.BAN_USER_WITH_OTP, req -> {
            AdminActionRequestDTO dto = (AdminActionRequestDTO) req.getPayload();
            AdminService.getInstance().banUser(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.BAN_USER_WITH_OTP, 200, "Đã khóa tài khoản thành công", null);
        });

        registry.put(ActionType.CANCEL_SESSION_WITH_OTP, req -> {
            AdminActionRequestDTO dto = (AdminActionRequestDTO) req.getPayload();
            AdminService.getInstance().cancelSession(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.CANCEL_SESSION_WITH_OTP, 200, "Đã hủy phiên đấu giá khẩn cấp", null);
        });

        // ==============================================================================
        // 3. NHÓM NGƯỜI BÁN & QUẢN LÝ VẬT PHẨM (SellerService & ItemManagementService)
        // ==============================================================================

        registry.put(ActionType.CREATE_ITEM, req -> {
            ItemCreateDTO dto = (ItemCreateDTO) req.getPayload();
            ItemManagementService.getInstance().createItemAndOpenSession(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.CREATE_ITEM, 200, "Đã tạo vật phẩm và mở phiên", null);
        });

        registry.put(ActionType.RELIST_ITEM, req -> {
            RelistRequestDTO dto = (RelistRequestDTO) req.getPayload();
            ItemManagementService.getInstance().relistUnsoldItem(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.RELIST_ITEM, 200, "Đăng bán lại thành công", null);
        });

        registry.put(ActionType.GET_SELLER_HISTORY, req -> {
            Object result = SellerService.getInstance().getSellerHistory(req.getUserId());
            return new ResponsePacket<>(ActionType.GET_SELLER_HISTORY, 200, "OK", result);
        });

        registry.put(ActionType.UPDATE_ITEM, req -> {
            ItemUpdateRequestDTO dto = (ItemUpdateRequestDTO) req.getPayload();
            SellerService.getInstance().updateItem(dto);
            return new ResponsePacket<>(ActionType.UPDATE_ITEM, 200, "Cập nhật vật phẩm thành công", null);
        });

        registry.put(ActionType.DELETE_ITEM, req -> {
            ItemTargetRequestDTO dto = (ItemTargetRequestDTO) req.getPayload();
            SellerService.getInstance().deleteItem(dto.getItemId());
            return new ResponsePacket<>(ActionType.DELETE_ITEM, 200, "Đã xóa vật phẩm thành công", null);
        });

        registry.put(ActionType.GET_SELLER_ITEMS, req -> {
            Object result = SellerService.getInstance().getItemsBySellerId(req.getUserId());
            return new ResponsePacket<>(ActionType.GET_SELLER_ITEMS, 200, "Lấy danh sách vật phẩm thành công", result);
        });

        // ==============================================================================
        // 4. NHÓM NGƯỜI MUA (BidderService & AuctionService)
        // ==============================================================================

        registry.put(ActionType.PRE_REGISTER_SESSION, req -> {
            SessionRegisterRequestDTO dto = (SessionRegisterRequestDTO) req.getPayload();
            BidderService.getInstance().registerSession(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.PRE_REGISTER_SESSION, 200, "Đăng ký tham gia phiên thành công", null);
        });

        registry.put(ActionType.DELETE_REGISTER_SESSION, req -> {
            SessionRegisterRequestDTO dto = (SessionRegisterRequestDTO) req.getPayload();
            BidderService.getInstance().cancelSessionRegistration(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.DELETE_REGISTER_SESSION, 200, "Hủy đăng ký tham gia phiên thành công", null);
        });

        registry.put(ActionType.GET_REGISTERED_SESSIONS, req -> {
            Object result = BidderService.getInstance().getRegisteredSessions(req.getUserId());
            return new ResponsePacket<>(ActionType.GET_REGISTERED_SESSIONS, 200, "OK", result);
        });

        registry.put(ActionType.GET_BIDDER_HISTORY, req -> {
            Object result = BidderService.getInstance().getBidderHistory(req.getUserId());
            return new ResponsePacket<>(ActionType.GET_BIDDER_HISTORY, 200, "OK", result);
        });

        registry.put(ActionType.GET_ACTIVE_SESSIONS, req -> {
            SessionFilterRequestDTO filter = (SessionFilterRequestDTO) req.getPayload();
            Object result = AuctionService.getInstance().getActiveSessions(filter);
            return new ResponsePacket<>(ActionType.GET_ACTIVE_SESSIONS, 200, "OK", result);
        });

        registry.put(ActionType.SEARCH_ITEMS, req -> {
            SessionFilterRequestDTO filter = (SessionFilterRequestDTO) req.getPayload();
            String categoryStr = filter.getCategory();
            Category category = (categoryStr == null || categoryStr.equalsIgnoreCase("ALL"))
                    ? null
                    : Category.valueOf(categoryStr.toUpperCase());
            Object result = ItemSearchService.getInstance().searchActiveAuctions(
                    filter.getKeyword(),
                    category,
                    filter.getTimeSortOption()
            );
            return new ResponsePacket<>(ActionType.SEARCH_ITEMS, 200, "OK", result);
        });

        registry.put(ActionType.GET_SESSION_DETAIL, req -> {
            SessionTargetRequestDTO dto = (SessionTargetRequestDTO) req.getPayload();
            Object result = AuctionService.getInstance().getItemDetail(dto.getSessionId());
            return new ResponsePacket<>(ActionType.GET_SESSION_DETAIL, 200, "OK", result);
        });

        registry.put(ActionType.JOIN_SESSION, req -> {
            SessionTargetRequestDTO dto = (SessionTargetRequestDTO) req.getPayload();
            AuctionRoomSyncDTO result = BidderService.getInstance().joinSession(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.JOIN_SESSION, 200, "Tham gia phòng đấu giá thành công", result);
        });

        registry.put(ActionType.LEAVE_SESSION, req -> {
            SessionTargetRequestDTO dto = (SessionTargetRequestDTO) req.getPayload();
            BidderService.getInstance().leaveSession(dto, req.getUserId());
            return new ResponsePacket<>(ActionType.LEAVE_SESSION, 200, "Đã thoát khỏi phòng đấu giá", null);
        });

        // ==============================================================================
        // 5. NHÓM CỐT LÕI ĐẤU GIÁ (AutoBidManager & AuctionService)
        // ==============================================================================

        registry.put(ActionType.PLACE_MANUAL_BID, req -> {
            BidRequestDTO dto = (BidRequestDTO) req.getPayload();
            InMemoryBidServiceImpl.getInstance().enqueueBid(dto, req.getUserId());

            return new ResponsePacket<>(ActionType.PLACE_MANUAL_BID, 200, "Yêu cầu trả giá đã được ghi nhận. Vui lòng chờ xử lý.", null);
        });

        registry.put(ActionType.REGISTER_AUTO_BID, req -> {
            AutoBidRegisterDTO dto = (AutoBidRegisterDTO) req.getPayload();
            AutoBidSetting setting = AutoBidMapper.toEntity(dto, req.getUserId());
            AutoBidManager.getInstance().registerAutoBid(setting);
            return new ResponsePacket<>(ActionType.REGISTER_AUTO_BID, 200, "Đã đăng ký trả giá tự động", null);
        });

        registry.put(ActionType.CANCEL_AUTO_BID, req -> {
            SessionTargetRequestDTO dto = (SessionTargetRequestDTO) req.getPayload();
            AutoBidManager.getInstance().cancelAutoBid(dto.getSessionId(), req.getUserId());
            return new ResponsePacket<>(ActionType.CANCEL_AUTO_BID, 200, "Đã tắt trả giá tự động", null);
        });

        // ==============================================================================
        // 6. NHÓM REALTIME SOCKET (RealtimeBroadcastService)
        // ==============================================================================

        registry.put(ActionType.SUBSCRIBE_REALTIME, req -> {
            SessionTargetRequestDTO dto = (SessionTargetRequestDTO) req.getPayload();
            RealtimeBroadcastService.getInstance().subscribe(dto.getSessionId(), req.getUserId());
            return new ResponsePacket<>(ActionType.SUBSCRIBE_REALTIME, 200, "Đã theo dõi phiên đấu giá", null);
        });

        registry.put(ActionType.UNSUBSCRIBE_REALTIME, req -> {
            SessionTargetRequestDTO dto = (SessionTargetRequestDTO) req.getPayload();
            RealtimeBroadcastService.getInstance().unsubscribe(dto.getSessionId(), req.getUserId());
            return new ResponsePacket<>(ActionType.UNSUBSCRIBE_REALTIME, 200, "Đã hủy theo dõi phiên", null);
        });
    }

    /**
     * Lấy Handler tương ứng với hành động.
     */
    public static CommandHandler getHandler(ActionType action) {
        return registry.getOrDefault(action, req -> {
            throw new BusinessException("Hành động [" + action + "] chưa được hỗ trợ hoặc là bản tin từ Server gửi xuống.");
        });
    }
}