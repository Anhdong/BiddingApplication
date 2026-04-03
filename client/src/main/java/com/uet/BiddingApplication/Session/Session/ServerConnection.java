package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

    private static volatile ServerConnection instance;

    private ServerConnection() {}

    public static ServerConnection getInstance(){
        if(instance == null ){
            synchronized (ServerConnection.class){
                if(instance == null){
                    instance = new ServerConnection();
                }
            }
        }
        return instance;
    }

    // Thuộc tính mạng
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    public void connect(String host, int port){
        // Logic kết nối sẽ dùng out và in mới
    }

    public void sendRequest(RequestPacket<?> packet){
        // Truyền JSON (nhớ nối thêm \n ở cuối theo chuẩn NDJSON)
    }

    public void listen(){

    }

    public void disconnect(){

    }
}