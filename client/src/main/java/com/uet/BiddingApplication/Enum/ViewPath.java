package com.uet.BiddingApplication.Enum;

public enum ViewPath {
    // AUTH (Không cần cache, đăng nhập xong là bỏ qua)
    LOGIN("/app/fxml/AuthView/LoginView.fxml", false),
    REGISTER("/app/fxml/AuthView/RegisterView.fxml", false),

    // MAINVIEW
    MAIN("/app/fxml/MainView.fxml", false),

    // SIDEBAR (Các thành phần này thường load 1 lần gắn cứng vào mép màn hình, không qua hệ thống chuyển tab)
    BIDDER_SIDEBAR("/app/fxml/SidebarView/BidderSidebarView.fxml", false),
    SELLER_SIDEBAR("/app/fxml/SidebarView/SellerSidebarView.fxml", false),
    ADMIN_SIDEBAR("/app/fxml/SidebarView/AdminSidebar.fxml", false),

    // BIDDER
    BIDDER_BROWSE("/app/fxml/BidderView/BidderBrowseView.fxml", false),   // CACHE: Cần mượt mà, giữ trạng thái Socket ngầm
    //ADD later
    BIDDER_WATCHLIST("",true),
    BIDDER_HISTORY("/app/fxml/BidderView/BidderHistoryView.fxml", false),  // CACHE: Danh sách tĩnh, ít khi thay đổi
    BIDDER_AUCTION("/app/fxml/BidderView/BidderAuctionView.fxml", false), // KHÔNG CACHE: Phải load mới 100% mỗi khi vào 1 phòng đấu giá khác nhau

    // SELLER
    //SELLER_ITEMS
    SELLER_ITEMS("",true),
    SELLER_ADD_ITEM("/app/fxml/SellerView/SellerAddItemView.fxml", true)  // CACHE: Để người dùng gõ dở thông tin lỡ ấn sang tab khác không bị mất form

    // ADMIN
    ;

    private final String path;
    private final boolean cacheable; // Thêm thuộc tính đánh dấu Cache

    // Cập nhật lại Constructor để bắt buộc truyền vào cờ boolean
    ViewPath(String path, boolean cacheable) {
        this.path = path;
        this.cacheable = cacheable;
    }

    public String getPath() {
        return path;
    }

    // Thêm Getter để MainController có thể gọi và kiểm tra
    public boolean isCacheable() {
        return cacheable;
    }

    public static ViewPath getSidebarView(RoleType role){
        return switch (role) {
            case RoleType.BIDDER -> ViewPath.BIDDER_SIDEBAR;
            case RoleType.SELLER -> ViewPath.SELLER_SIDEBAR;
            case RoleType.ADMIN  -> ViewPath.ADMIN_SIDEBAR;
        };
    }

    public static ViewPath getDefaultView(RoleType role){
        return switch (role) {
        case RoleType.BIDDER -> ViewPath.BIDDER_BROWSE;
        case RoleType.SELLER -> ViewPath.SELLER_ITEMS;
        case RoleType.ADMIN  -> null;//temp
        };
    }
}