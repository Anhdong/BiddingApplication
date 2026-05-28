package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.ItemCreateDTO;
import com.uet.BiddingApplication.DTO.Request.RelistRequestDTO;
import com.uet.BiddingApplication.DTO.Request.SessionRegisterRequestDTO;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.SessionRegistration;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuctionSessionMapper {

    /**
     * Trích xuất phần thời gian, giá khởi điểm từ Request và gán itemId vừa tạo để lưu DB.
     */
    public static AuctionSession toEntity(ItemCreateDTO dto, String itemId, String sellerId){
        if (dto == null) return null;

        AuctionSession entity = new AuctionSession();

        entity.setSellerId(sellerId);

        entity.setItemId(itemId);

        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setStartPrice(dto.getStartPrice());
        entity.setBidStep(dto.getBidStep());

        entity.setCurrentPrice(dto.getStartPrice());

        entity.setStatus(SessionStatus.OPEN);

        return entity;
    }

    public static AuctionSession toEntity(RelistRequestDTO dto, String sellerId){
        if (dto == null) return null;

        AuctionSession entity = new AuctionSession();

        String itemId = dto.getItemId();

        entity.setSellerId(sellerId);

        entity.setItemId(itemId);

        entity.setStartTime(dto.getNewStartTime());
        entity.setEndTime(dto.getNewEndTime());
        entity.setStartPrice(dto.getNewStartPrice());
        entity.setBidStep(dto.getNewBidStep());

        entity.setCurrentPrice(dto.getNewStartPrice());

        entity.setStatus(SessionStatus.OPEN);

        return entity;
    }

    public static SessionRegistration toEntity(SessionRegisterRequestDTO requestDTO, String bidderId){
        SessionRegistration registration = new SessionRegistration();
        registration.setId(UUID.randomUUID().toString());
        registration.setBidderId(bidderId);
        registration.setSessionId(requestDTO.getSessionId());
        registration.setCreatedAt(LocalDateTime.now());

        return registration;
    }
}