package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.BidderHistoryResponseDTO;
import com.uet.BiddingApplication.Model.BidTransaction;

import java.util.List;

public interface IBidDAO {
    boolean insertBid(BidTransaction bid);
    List<BidHistoryDTO> getRecentBids(String sessionId);
    List<BidderHistoryResponseDTO> getBidderHistory(String bidderId);
}
