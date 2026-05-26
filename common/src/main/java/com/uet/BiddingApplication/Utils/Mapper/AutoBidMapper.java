package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Request.AutoBidRegisterDTO;
import com.uet.BiddingApplication.Model.AutoBidSetting;

import java.time.LocalDateTime;
import java.util.UUID;

public class AutoBidMapper {

    public static AutoBidSetting toEntity(AutoBidRegisterDTO dto, String bidderId){
        AutoBidSetting entity = new AutoBidSetting();

        entity.setId(UUID.randomUUID().toString());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setBidderId(bidderId);
        entity.setBidderName(dto.getBidderName());
        entity.setIncrement(dto.getIncrement());
        entity.setMaxBid(dto.getMaxBid());
        entity.setSessionId(dto.getSessionId());

        return  entity;
    }
}
