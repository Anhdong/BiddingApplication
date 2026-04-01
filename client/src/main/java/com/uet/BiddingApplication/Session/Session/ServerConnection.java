package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {
    public static volatile ServerConnection instance;
    private ServerConnection() {}
    public static ServerConnection getInstance(){
        if(instance == null ){
            synchronized (ClientSession.class){
                if(instance == null){
                    instance = new ServerConnection();
                }
            }
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
