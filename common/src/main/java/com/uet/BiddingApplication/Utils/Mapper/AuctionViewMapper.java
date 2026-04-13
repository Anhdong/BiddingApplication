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


    /**
     * Gộp AuctionSession và Item thành DTO chi tiết để hiển thị cho Client.
     * Áp dụng tính đa hình và tra cứu cache để bảo mật & tối ưu hiệu suất. [cite: 1099]
     */
    public static SessionInfoResponseDTO toDetailDto(AuctionSession session, Item item) {
        if (session == null || item == null) return null;

        // 1. Khởi tạo DTO và ánh xạ các trường cơ bản từ AuctionSession
        SessionInfoResponseDTO dto = new SessionInfoResponseDTO();
        dto.setSessionId(session.getId());
        dto.setStartPrice(session.getStartPrice());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setStatus(session.getStatus());

        // 2. Ánh xạ thông tin vật phẩm từ Item
        dto.setItemName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setImageUrl(item.getImageURL());
        dto.setCategory(item.getCategory());

        // 4. Đa hình thuộc tính sản phẩm
        dto.setAttribute(item.getDisplayAttributes());
        return dto;
    }
}
