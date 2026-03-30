package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;

public class ResponseDispatcher {
    private static ResponseDispatcher instance;

    private ResponseDispatcher() {}

    public static synchronized ResponseDispatcher getInstance() {
        if (instance == null) {
            instance = new ResponseDispatcher();
        }
        return instance;
    }
    public void dispatch(ResponsePacket<?> response){
        //switch(response.getAction());
        //Platform.runLater()
    }
}
