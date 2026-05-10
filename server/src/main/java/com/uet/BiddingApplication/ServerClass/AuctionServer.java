package com.uet.BiddingApplication.ServerClass;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuctionServer.class);

    private static volatile AuctionServer instance;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, ClientConnectionHandler> clients;

    private volatile boolean running = false;

    private Thread udpDiscoveryThread;

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
        startUDPDiscovery();
        try {
            serverSocket = new ServerSocket(port);

            // Thử dùng Virtual Threads nếu JVM hỗ trợ, nếu không fallback sang cached pool
            try {
                threadPool = Executors.newVirtualThreadPerTaskExecutor();
                log.info("Using Virtual Threads executor");
            } catch (Throwable t) {
                threadPool = Executors.newCachedThreadPool();
                log.info("Virtual Threads not available, using cached thread pool");
            }

            running = true;
            log.info("AuctionServer started on port " + port);

            while (running) {
                Socket socket = serverSocket.accept();
                // Tạo handler và giao cho thread pool xử lý
                ClientConnectionHandler handler = new ClientConnectionHandler(socket, this);
                threadPool.execute(handler);
                // **Không** put vào clients ở đây vì userId có thể chưa biết (chưa login)
            }
        } catch (IOException e) {
            if (running) {
                log.error("Đã xảy ra lỗi Exception:", e);
            } else {
                log.info("Server stopped.");
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;

        if (udpDiscoveryThread != null) {
            udpDiscoveryThread.interrupt();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Đã xảy ra lỗi Exception:", e);
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        // Đóng tất cả client
        for (ClientConnectionHandler handler : clients.values()) {
            try {
                handler.closeConnection();
            } catch (Exception ignored) {}
        }
        clients.clear();
    }

    /**
     * Đăng ký client khi đã có userId (gọi từ ClientConnectionHandler sau khi login thành công)
     */
    public void registerClient(String userId, ClientConnectionHandler handler) {
        if (userId == null || handler == null) return;
        clients.put(userId, handler);
        log.info("Registered client: " + userId);
    }

    /**
     * Hủy đăng ký client (gọi khi client disconnect hoặc bị kick)
     */
    public void unregisterClient(String userId) {
        if (userId == null) return;
        clients.remove(userId);
        log.info("Unregistered client: " + userId);
    }

    public ClientConnectionHandler getClientHandler(String userId) {
        return clients.get(userId);
    }

    /**
     * Kick user: xóa khỏi map và đóng kết nối
     */
    public void kickUser(String userId) {
        if (userId == null) return;
        ClientConnectionHandler handler = clients.remove(userId);
        if (handler != null) {
            try {
                handler.closeConnection();
            } catch (Exception e) {
                log.error("Đã xảy ra lỗi Exception:", e);
            }
            log.info("User " + userId + " has been kicked.");
        } else {
            log.info("kickUser: user not found: " + userId);
        }
    }
    private void startUDPDiscovery() {
        // Khởi tạo luồng UDP và đánh dấu là Daemon
        UDPDiscoveryServer udpTask = new UDPDiscoveryServer();
        udpDiscoveryThread = new Thread(udpTask);

        // Cực kỳ quan trọng: Daemon giúp luồng này tự chết nếu JVM dừng
        udpDiscoveryThread.setDaemon(true);
        udpDiscoveryThread.start();
    }
}