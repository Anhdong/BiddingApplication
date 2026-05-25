package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.Model.AutoBidSetting;

import java.util.List;

public interface IAutoBidSettingDAO {
    boolean upsertAutoBid(AutoBidSetting autoBid);
    boolean deleteAutoBid(String bidderId, String sessionId);
    AutoBidSetting getAutoBid(String bidderId, String sessionId);
    boolean deleteAllBySessionId(String sessionId);
    List<AutoBidSetting> getAllAutoBids();
    boolean deleteAllByBidderId(String bidderId);
}
