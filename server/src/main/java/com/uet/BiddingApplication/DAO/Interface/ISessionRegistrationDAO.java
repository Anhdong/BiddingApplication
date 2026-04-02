package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;

import java.util.List;

public interface ISessionRegistrationDAO {
    boolean registerBidder(String bidderId,String sessionId);
    boolean checkRegistration(String bidderId,String sessionId);
    boolean deleteRegistration(String bidderId,String sessionId);
    List<AuctionCardDTO> getRegisteredSessions(String bidderId);
}
