package com.uet.BiddingApplication.Utils;

import com.google.gson.reflect.TypeToken;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.DTO.Response.*;
import com.uet.BiddingApplication.DTO.Request.*;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry quản lý cấu trúc dữ liệu mạng.
 * Áp dụng nguyên tắc OCP (Open-Closed Principle): Thêm Action mới chỉ cần khai báo ở đây,
 * không được sửa core logic của Parser.
 */
public class PacketTypeRegistry {

    private static final Map<ActionType, Type> requestTypes = new EnumMap<>(ActionType.class);
    private static final Map<ActionType, Type> responseTypes = new EnumMap<>(ActionType.class);

    static {
        registerRequestMappings();
        registerResponseMappings();
    }

    /**
     * KHỐI 1: CẤU HÌNH REQUEST (CLIENT GỬI -> SERVER ĐỌC)
     */
    private static void registerRequestMappings() {
        // --- Nhóm Auth & Profile ---
        requestTypes.put(ActionType.LOGIN, AuthRequestDTO.class);
        requestTypes.put(ActionType.REGISTER, RegisterRequestDTO.class);
        requestTypes.put(ActionType.LOGOUT, Void.class); // Explicitly chặn payload
        requestTypes.put(ActionType.CHANGE_PASSWORD, PasswordChangeRequestDTO.class);
        requestTypes.put(ActionType.UPDATE_PROFILE, ProfileUpdateRequestDTO.class);

        // --- Nhóm Room & Socket ---
        Type targetRequestType = SessionTargetRequestDTO.class;
        requestTypes.put(ActionType.JOIN_SESSION, targetRequestType);
        requestTypes.put(ActionType.LEAVE_SESSION, targetRequestType);
        requestTypes.put(ActionType.SUBSCRIBE_REALTIME, targetRequestType);
        requestTypes.put(ActionType.UNSUBSCRIBE_REALTIME, targetRequestType);

        // --- Nhóm Admin ---
        requestTypes.put(ActionType.GET_ALL_USERS, Void.class);
        requestTypes.put(ActionType.GET_ALL_SESSIONS, Void.class);
        requestTypes.put(ActionType.REQUEST_OTP, Void.class);
        requestTypes.put(ActionType.BAN_USER_WITH_OTP, AdminActionRequestDTO.class);
        requestTypes.put(ActionType.CANCEL_SESSION_WITH_OTP, AdminActionRequestDTO.class);

        // --- Nhóm Seller ---
        requestTypes.put(ActionType.CREATE_ITEM, ItemCreateDTO.class);
        requestTypes.put(ActionType.UPDATE_ITEM, ItemUpdateRequestDTO.class);
        requestTypes.put(ActionType.DELETE_ITEM, ItemTargetRequestDTO.class); // Đã update DTO mới
        requestTypes.put(ActionType.RELIST_ITEM, RelistRequestDTO.class);
        requestTypes.put(ActionType.GET_SELLER_HISTORY, Void.class);

        // --- Nhóm Bidder ---
        Type filterRequestType = SessionFilterRequestDTO.class;
        requestTypes.put(ActionType.GET_ACTIVE_SESSIONS, filterRequestType);
        requestTypes.put(ActionType.SEARCH_ITEMS, filterRequestType);
        requestTypes.put(ActionType.GET_SESSION_DETAIL, targetRequestType);
        requestTypes.put(ActionType.PRE_REGISTER_SESSION, SessionRegisterRequestDTO.class);
        requestTypes.put(ActionType.DELETE_REGISTER_SESSION, SessionRegisterRequestDTO.class);
        requestTypes.put(ActionType.GET_REGISTERED_SESSIONS, Void.class);
        requestTypes.put(ActionType.GET_BIDDER_HISTORY, Void.class);

        // --- Nhóm Core Auction ---
        requestTypes.put(ActionType.PLACE_MANUAL_BID, BidRequestDTO.class);
        requestTypes.put(ActionType.REGISTER_AUTO_BID, AutoBidRegisterDTO.class);
        requestTypes.put(ActionType.CANCEL_AUTO_BID, targetRequestType);
    }

    /**
     * KHỐI 2: CẤU HÌNH RESPONSE (SERVER GỬI -> CLIENT ĐỌC)
     */
    private static void registerResponseMappings() {
        // 1. Các List phức tạp (Xử lý Generic Type Erasure)
        Type userProfileListType = new TypeToken<List<UserProfileDTO>>(){}.getType();
        Type auctionCardListType = new TypeToken<List<AuctionCardDTO>>(){}.getType();
        Type sellerHistoryListType = new TypeToken<List<SellerHistoryResponseDTO>>(){}.getType();
        Type bidderHistoryListType = new TypeToken<List<BidderHistoryResponseDTO>>(){}.getType();

        // --- Nhóm Auth & Profile ---
        responseTypes.put(ActionType.LOGIN, AuthResponseDTO.class);
        responseTypes.put(ActionType.UPDATE_PROFILE, UserProfileDTO.class);
        responseTypes.put(ActionType.JOIN_SESSION, AuctionRoomSyncDTO.class);
        responseTypes.put(ActionType.FORCE_LOGOUT, String.class); // Payload là chuỗi lý do khóa

        // --- Nhóm Admin ---
        responseTypes.put(ActionType.GET_ALL_USERS, userProfileListType); // Tránh rò rỉ Entity User
        responseTypes.put(ActionType.GET_ALL_SESSIONS, auctionCardListType); // Tránh rò rỉ Entity Session

        // --- Nhóm Seller & Bidder ---
        responseTypes.put(ActionType.GET_SELLER_HISTORY, sellerHistoryListType);
        responseTypes.put(ActionType.GET_BIDDER_HISTORY, bidderHistoryListType);
        responseTypes.put(ActionType.GET_ACTIVE_SESSIONS, auctionCardListType);
        responseTypes.put(ActionType.SEARCH_ITEMS, auctionCardListType);
        responseTypes.put(ActionType.GET_REGISTERED_SESSIONS, auctionCardListType);
        responseTypes.put(ActionType.GET_SESSION_DETAIL, SessionInfoResponseDTO.class);

        // --- Nhóm Realtime ---
        responseTypes.put(ActionType.REALTIME_PRICE_UPDATE, RealtimeUpdateDTO.class);
        responseTypes.put(ActionType.REALTIME_SESSION_END, SessionResultDTO.class);
        responseTypes.put(ActionType.AUTO_BID_CANCEL, AutoBidCancelResponseDTO.class);

        /*
         * LƯU Ý QUAN TRỌNG:
         * Tất cả các Action trả về SUCCESS/200 nhưng không cần data (như CREATE_ITEM,
         * CANCEL_AUTO_BID, REGISTER, LOGOUT,...) sẽ tự động fallback về Void.class
         * thông qua hàm getResponseType. Không cần cấu hình thừa thãi.
         */
    }

    /**
     * Tra cứu với thời gian O(1)
     */
    public static Type getRequestType(ActionType action) {
        return requestTypes.getOrDefault(action, Void.class); // Mặc định là Void để chống JSON Injection
    }

    public static Type getResponseType(ActionType action) {
        return responseTypes.getOrDefault(action, Void.class);
    }
}