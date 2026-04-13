package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Enum.SessionStatus; // Giả sử em có Enum này

public class AuctionSessionMapper {

    /**
     * Trích xuất phần thời gian, giá khởi điểm từ Request và gán itemId vừa tạo để lưu DB.
     */
    public static AuctionSession toEntity(ItemCreateDTO dto, String itemId){
        // 1. Kiểm tra an toàn
        if (dto == null) return null;

        AuctionSession entity = new AuctionSession();

        // 2. Gắn khóa ngoại (Foreign key)
        entity.setItemId(itemId);

        // 3. Trích xuất thông tin phiên
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setStartPrice(dto.getStartPrice());

        // 4. BỔ SUNG LOGIC NGHIỆP VỤ (Quan trọng)
        // Giá hiện tại ban đầu chính là giá khởi điểm
        entity.setCurrentPrice(dto.getStartPrice());

        // Gán trạng thái ban đầu cho phiên (Chưa bắt đầu hoặc Đang mở)
        // Tùy theo logic của nhóm em có thể là PENDING hoặc OPEN
        entity.setStatus(SessionStatus.OPEN);

        return entity;
    }
}