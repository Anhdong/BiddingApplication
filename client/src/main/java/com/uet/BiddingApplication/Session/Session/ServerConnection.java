package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;

    private ServerConnection() {} // Chặn tạo object mới

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;

    public void connect(String host, int port){

    }
    public void sendRequest(RequestPacket<?> packet){

    }
    public void listen(){

    }
    public void disconnect(){

    }


}
