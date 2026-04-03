package com.uet.BiddingApplication.Server_class;

import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class AuctionServer {

    private static volatile AuctionServer instance;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, ClientConnectionHandler> clients;

    private AuctionServer() {
        clients = new ConcurrentHashMap<>();
    }

    public static AuctionServer getInstance() {
        if (instance == null) {
            synchronized (AuctionServer.class) {
                if (instance == null) {
                    instance = new AuctionServer();
                }
            }
        }
        return instance;
    }

    public void start(int port) {
        // TODO: Khởi tạo ServerSocket và vòng lặp chấp nhận kết nối (dùng Virtual Threads)
    }

    public ClientConnectionHandler getClientHandler(String userId) {
        // TODO: Lấy luồng xử lý của Client từ danh sách
        return null;
    }

    public void kickUser(String userId) {
        // TODO: Ép đóng kết nối và xóa Client khỏi danh sách
    }
}