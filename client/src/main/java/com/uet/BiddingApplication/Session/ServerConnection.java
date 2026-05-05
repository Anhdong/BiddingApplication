package com.uet.BiddingApplication.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.Utils.GsonPacketParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

    // Singleton pattern
    private static volatile ServerConnection instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private ResponseListenerThread listenerThread;
    private Thread threadHandle;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            synchronized (ServerConnection.class) {
                if (instance == null) {
                    instance = new ServerConnection();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // THÊM MỚI: Tự động lấy host+port từ GitHub Gist rồi kết nối
    // Gọi method này thay cho connect(host, port) khi dùng Ngrok
    // -------------------------------------------------------------------------
    public void connectFromRemoteConfig() {
        try {
            System.out.println("[ServerConnection] Đang tải cấu hình từ Gist...");
            RemoteConfigFetcher.ServerAddress addr = RemoteConfigFetcher.fetch();
            System.out.println("[ServerConnection] Cấu hình nhận được: " + addr);
            connect(addr.host, addr.port);
        } catch (Exception e) {
            System.err.println("[ServerConnection] Không thể tải cấu hình: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Giữ nguyên — giờ được gọi bởi cả connectFromRemoteConfig() lẫn code cũ
    // -------------------------------------------------------------------------
    public void connect(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            this.listenerThread = new ResponseListenerThread(in);
            this.threadHandle = new Thread(listenerThread);
            this.threadHandle.setDaemon(true);
            this.threadHandle.start();

            System.out.println("Đã kết nối Server thành công và bật luồng lắng nghe!");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Server: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (listenerThread != null) listenerThread.stopListening();
            if (threadHandle != null) threadHandle.interrupt();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Đã ngắt kết nối an toàn.");
        } catch (Exception e) {
            System.err.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    public void sendRequest(RequestPacket<?> request) {
        if (out != null) {
            try {
                String jsonStr = GsonPacketParser.serialize(request);
                out.println(jsonStr);
            } catch (Exception e) {
                System.err.println("Lỗi đóng gói dữ liệu: " + e.getMessage());
            }
        } else {
            System.err.println("Chưa kết nối Server, không thể gửi request!");
        }
    }
}