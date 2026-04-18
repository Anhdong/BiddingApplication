package com.uet.BiddingApplication.Enum;

public enum ActionType {
    // Nhóm Xác thực & Cá nhân (Auth & Profile - 5 hành động)
    LOGIN, // Đăng nhập
    REGISTER, // Đăng ký
    LOGOUT, // Đăng xuất khỏi Session
    CHANGE_PASSWORD, // Đổi mật khẩu trong Settings
    UPDATE_PROFILE, // Cập nhật thông tin: Address, Phone...
    JOIN_SESSION, //Gửi khi Bidder nhấn nút vào phòng.
    LEAVE_SESSION,//Gửi khi Bidder chủ động thoát phòng.
    FORCE_LOGOUT, //Server bắn xuống để thông báo tài khoản bị "đá" hoặc bị khóa.

    // Nhóm Admin (Quản trị viên - 5 hành động)
    GET_ALL_USERS, // Lấy danh sách User để quản lý
    GET_ALL_SESSIONS, // Lấy toàn bộ phiên đấu giá
    REQUEST_OTP, // Yêu cầu Server sinh mã OTP và in ra console
    BAN_USER_WITH_OTP, // Khóa tài khoản kèm mã xác thực
    CANCEL_SESSION_WITH_OTP, // Hủy phiên khẩn cấp kèm mã xác thực

    // Nhóm Seller (Người bán - 5 hành động)
    CREATE_ITEM, // Tạo vật phẩm & Mở phiên đấu giá
    UPDATE_ITEM, // Sửa thông tin vật phẩm khi phiên chưa chạy
    DELETE_ITEM, // Xóa vật phẩm khỏi kho
    RELIST_ITEM, // Đăng bán lại vật phẩm bị ế
    GET_SELLER_HISTORY, // Lấy lịch sử các phiên của Seller kèm tên Winner

    // Nhóm Bidder (Thông tin & Tìm kiếm - 6 hành động)
    GET_ACTIVE_SESSIONS, // Lấy danh sách phiên đang chạy cho trang chủ
    SEARCH_ITEMS, // Tìm kiếm và lọc theo Keyword/Category/Price
    GET_SESSION_DETAIL, // Mở xem chi tiết 1 vật phẩm
    PRE_REGISTER_SESSION, // Bấm nút "Đăng ký trước" để được phép đấu giá
    DELETE_REGISTER_SESSION,// Bấm nút "Hủy đăng ký trước"
    GET_REGISTERED_SESSIONS, // Lấy danh sách các phiên Bidder đã đăng ký
    GET_BIDDER_HISTORY, // Lấy kết quả thắng/thua các phiên đã tham gia

    // Nhóm Đấu giá (Core Auction - 3 hành động)
    PLACE_MANUAL_BID, // Người dùng tự nhập giá và gửi lên
    REGISTER_AUTO_BID, // Cài đặt giá Max và Bước nhảy tự động
    CANCEL_AUTO_BID, // Tắt Auto-bid giữa chừng

    // Nhóm Tương tác Socket Real-time (2 hành động)
    SUBSCRIBE_REALTIME, // Client xin Server: "Báo cho tôi giá mới của phòng X"
    UNSUBSCRIBE_REALTIME, // Client báo: "Tôi thoát phòng X, đừng gửi tin nữa"

    // Nhóm Phản hồi từ Server xuống Client (Server Responses - 4 hành động)
    REALTIME_SESSION_STARTED, // Thông báo phiên bắt đầu
    REALTIME_SESSION_CANCELED,// Thông báo khi phiên bị hủy
    REALTIME_PRICE_UPDATE, // Server tự động đẩy giá mới và người giữ giá xuống
    REALTIME_SESSION_END, // Server tự động báo hiệu hết giờ và chốt Winner
    AUTO_BID_CANCEL // Thông báo cho bidder khi giá hiện tại vượt quá max bid
}
