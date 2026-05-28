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
            cardDTOList.add(toCardDTO(session,itemCache.get(session.getItemId())));
        }
        return cardDTOList;
    }


    /**
     * Gộp AuctionSession và Item thành DTO chi tiết để hiển thị cho Client.
     * Áp dụng tính đa hình và tra cứu cache để bảo mật & tối ưu hiệu suất.
     */
    public static SessionInfoResponseDTO toDetailDto(AuctionSession session, Item item) {
        if (session == null || item == null) return null;

        SessionInfoResponseDTO dto = new SessionInfoResponseDTO();
        dto.setSessionId(session.getId());
        dto.setItemId(item.getId());
        dto.setStartPrice(session.getStartPrice());
        dto.setBidStep(session.getBidStep());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setStatus(session.getStatus());

        dto.setItemName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setImageUrl(item.getImageURL());
        dto.setCategory(item.getCategory());

        dto.setAttribute(item.getDisplayAttributes());
        return dto;
    }
}
