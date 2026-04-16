package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Model.Item;

import java.math.BigDecimal;
import java.util.List;

public interface ISearchCacheManager {
    void loadInitialData();
    void loadInitialItems();

    List<AuctionCardDTO> getAllActiveSessionsAsCardDto();
    SessionInfoResponseDTO getSessionDetailDto(String sessionId);

    void addSessionAndItem(AuctionSession auctionSession,Item item);
    void removeSession(String sessionId);
    void removeItem(String itemId);
    void updatePriceInCache(String sessionId, BigDecimal newPrice,String highestBidderId);
    Item getItem(String itemId);
    AuctionSession getSession(String sessionId);
    List<AuctionSession> getActiveSessions();
    List<AuctionSession> getSessionsByStatus(SessionStatus status);
}

