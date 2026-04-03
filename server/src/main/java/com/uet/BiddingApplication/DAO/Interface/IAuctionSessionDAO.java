package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.DTO.Response.SellerHistoryResponseDTO;
import com.uet.BiddingApplication.DTO.Response.SessionInfoResponseDTO;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.AuctionSession;

import java.math.BigDecimal;
import java.util.List;

public interface IAuctionSessionDAO {
    boolean insertSession(AuctionSession session);
    AuctionSession getSessionById(String sessionId);
    boolean updatePriceAndWinner(String sessionId, BigDecimal newPrice, String winnerId);
    boolean updateStatus(String sessionId, SessionStatus status);
    List<AuctionSession> getAllSessions(boolean isActive);
    List<SellerHistoryResponseDTO> getSellerHistory(String sellerId);
    SessionInfoResponseDTO getSessionInfo(String sessionId);
}
