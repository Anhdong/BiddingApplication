package com.uet.BiddingApplication.Interface;

/**
 * Interface này giúp MainViewController biết cách "đánh thức"
 * hoặc "cho đi ngủ" các Controller khi chuyển qua lại giữa các tab.
 */
public interface ViewControllerLifecycle {

    // Hàm này chạy mỗi khi màn hình ĐƯỢC HIỂN THỊ (kể cả load mới hay lôi từ cache ra)
    default void onShow() {}

    // Hàm này chạy mỗi khi màn hình BỊ CHE KHUẤT hoặc chuyển sang màn hình khác
    default void onHide() {}
}