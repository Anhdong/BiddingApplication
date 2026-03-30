package com.uet.BiddingApplication.Server_class;

import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class AuctionServer {

    private static AuctionServer instance;

    private AuctionServer() {}

    public static synchronized AuctionServer getInstance() {
        if (instance == null) {
            instance = new AuctionServer();
        }
        return instance;
    }

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, ClientConnectionHandler> clients;

    public void start(int port) {

    }

    public ClientConnectionHandler getClientHandler(String userId) {
        return null;
    }

    public void kickUser(String userId) {

    }
}