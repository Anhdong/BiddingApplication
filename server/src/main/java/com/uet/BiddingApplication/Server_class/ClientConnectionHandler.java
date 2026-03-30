package com.uet.BiddingApplication.Server_class;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConnectionHandler implements Runnable {


    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String userId;


    public ClientConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

    }
    public void sendPacket(ResponsePacket<?> packet) {

    }
    public void forceClose() {

    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
}