package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;

public class ResponseDispatcher {
    public static volatile ResponseDispatcher instance;
    private ResponseDispatcher() {}
    public static ResponseDispatcher getInstance(){
        if(instance == null ){
            synchronized (ClientSession.class){
                if(instance == null){
                    instance = new ResponseDispatcher();
                }
            }
        }
        return instance;
    }
    public void dispatch(ResponsePacket<?> response){
        //switch(response.getAction());
        //Platform.runLater()
    }
}
