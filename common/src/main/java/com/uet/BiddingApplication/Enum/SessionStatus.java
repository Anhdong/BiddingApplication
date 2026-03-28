package com.uet.BiddingApplication.Enum;

public enum SessionStatus {
    OPEN, // Đang mở cho Bidder đăng ký trước, chưa đấu giá được
    RUNNING, // Đang đếm ngược, cho phép trả giá
    FINISHED, // Đã chốt người thắng hoặc không có ai mua
    CANCELED // Bị Admin hủy bỏ khẩn cấp
}