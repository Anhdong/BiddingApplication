package com.uet.BiddingApplication.Utils.Mapper;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuctionViewMapper {
    public static AuctionCardDTO toCardDTO(AuctionSession session, Item item){
        AuctionCardDTO cardDTO = new AuctionCardDTO(
                session.getId(),
                item.getName(),
                item.getImageURL(),
                session.getStartPrice(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus()
        );
        return cardDTO;
    }
    public static List<AuctionCardDTO> toCardDTOList(List<AuctionSession> sessions, Map<String,Item> itemCache){
        List<AuctionCardDTO> cardDTOList = new ArrayList<>();
        for(AuctionSession session : sessions){
            cardDTOList.add(toCardDTO(session,itemCache.get(session.getId())));
        }
        return cardDTOList;
    }
}
