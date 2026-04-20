package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.Session.Session.ResponseListenerThread;
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

    public void connect(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            // Khởi tạo và chạy Luồng Lắng Nghe độc lập
            this.listenerThread = new ResponseListenerThread(in);
            this.threadHandle = new Thread(listenerThread);
            this.threadHandle.setDaemon(true); // Tự động chết khi app đóng
            this.threadHandle.start();

            System.out.println("Đã kết nối Server thành công và bật luồng lắng nghe!");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Server: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            // 1. Ra lệnh cho luồng nghe dừng lại
            if (listenerThread != null) {
                listenerThread.stopListening();
            }
            // 2. Ép luồng dừng hẳn (nếu đang bị block ở readLine)
            if (threadHandle != null) {
                threadHandle.interrupt();
            }
            // 3. Đóng ống nước và Socket
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("Đã ngắt kết nối an toàn.");
        } catch (Exception e) {
            System.err.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    // Hàm thay thế cho out.print() rải rác ở khắp nơi
    public void sendRequest(RequestPacket<?> request) {
        if (out != null) {
            try {
                String jsonStr = GsonPacketParser.serialize(request);
                out.println(jsonStr);
                // System.out.println("[Client -> Server] Đã gửi: " + jsonStr);
            } catch (Exception e) {
                System.err.println("Lỗi đóng gói dữ liệu: " + e.getMessage());
            }
        } else {
            System.err.println("Chưa kết nối Server, không thể gửi request!");
        }
    }
}