package com.uet.BiddingApplication.Controller.Enum;

public enum ViewPath {
    //AUTH
    LOGIN("/app/fxml/AuthView/LoginView.fxml"),
    REGISTER("/app/fxml/AuthView/RegisterView.fxml"),

    //MAINVIEW
    MAIN("/app/fxml/MainView.fxml"),

    //SIDEBAR
    BIDDER_SIDEBAR("/app/fxml/SidebarView/BidderSidebar.fxml"),
    SELLER_SIDEBAR("/app/fxml/SidebarView/SellerSidebar.fxml"),
    ADMIN_SIDEBAR("/app/fxml/SidebarView/AdminSidebar.fxml"),

    //BIDDER
    BIDDER_BROWSE("/app/fxml/BidderView/BrowseView.fxml"),
    //BIDDER_WATCHLIST
    BIDDER_HISTORY("/app/fxml/BidderView/HistoryView.fxml"),
    BIDDER_AUCTION("/app/fxml/BidderView/AuctionView.fxml"),

    //SELLER
    //SELLER_ITEMS
    SELLER_ADD_ITEM("/app/fxml/SellerView/AddItemView.fxml")

    //ADMIN
    ;


    private final String path;

    ViewPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}