package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Model.SessionRegistration;

import java.util.List;

public interface ISessionRegistrationDAO {
    boolean registerBidder(SessionRegistration registration);
    boolean checkRegistration(String bidderId,String sessionId);
    boolean deleteRegistration(String bidderId,String sessionId);
    List<AuctionCardDTO> getRegisteredSessions(String bidderId);
}
