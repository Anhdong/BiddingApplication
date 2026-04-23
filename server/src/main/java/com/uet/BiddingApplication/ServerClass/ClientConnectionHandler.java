package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
// Sửa Import này để dùng đúng bộ Parser ở Utils
import com.uet.BiddingApplication.Utils.GsonPacketParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;

    private BufferedReader in;
    private PrintWriter out;

    private String userId;
//Lắng nghe chuỗi JSON từ mạng -> Dịch thành Java Object
// -> Giao cho Router xử lý -> Nhận kết quả -> Dịch lại thành JSON -> Gửi trả Client
    public ClientConnectionHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
        try {
            // Nên chỉ định rõ UTF-8 để tránh lỗi font tiếng Việt khi chạy trên các OS khác nhau
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
            forceClose();
        }
    }

    @Override
    public void run() {
        try {
            String jsonLine;
            while ((jsonLine = in.readLine()) != null) {
                System.out.println("[Server nhận từ " + (userId != null ? userId : "Guest") + "] " + jsonLine);

                // SỬA TẠI ĐÂY: Dùng hàm deserializeRequest từ bản Utils
                RequestPacket<?> requestPacket = GsonPacketParser.deserializeRequest(jsonLine);

                if (requestPacket != null) {
                    // Không cần Import RequestRouter vì nó cùng package Server_class
                    RequestRouter.getInstance().route(requestPacket, this);
                }
            }
        } catch (Exception e) {
            System.err.println("[Handler] Kết nối với " + userId + " bị ngắt: " + e.getMessage());
        } finally {
            forceClose();
            // Hủy đăng ký client khi ngắt kết nối
            if (server != null && userId != null) {
                server.unregisterClient(userId);
            }
        }
    }

    /**
     * Gửi ResponsePacket về client
     */
    public synchronized void sendPacket(ResponsePacket<?> packet) {
        if (out == null) return;
        try {
            // SỬA TẠI ĐÂY: Dùng hàm serialize tập trung ở Utils cho đồng bộ
            String jsonStr = GsonPacketParser.serialize(packet);
            out.print(jsonStr);
            out.flush();
        } catch (Exception e) {
            System.err.println("[Handler] Lỗi gửi gói tin: " + e.getMessage());
        }
    }

    public void forceClose() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        forceClose();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        if (userId != null && server != null) {
            server.registerClient(userId, this);
        }
    }
}