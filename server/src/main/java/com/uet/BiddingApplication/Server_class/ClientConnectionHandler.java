package com.uet.BiddingApplication.Server_class;

import com.google.gson.Gson;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Router.RequestRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;

    // V2.0: Dùng luồng Text thay cho ObjectStream
    private BufferedReader in;
    private PrintWriter out;

    private String userId;
    private final Gson gson = new Gson();

    public ClientConnectionHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true
        } catch (IOException e) {
            e.printStackTrace();
            forceClose();
        }
    }

    @Override
    public void run() {
        try {
            String jsonLine;
            // Vòng lặp liên tục lắng nghe từ Client
            while ((jsonLine = in.readLine()) != null) {
                System.out.println("[Server nhận] " + jsonLine);

                // Dòng này hết đỏ vì ta đã catch Exception ở dưới
                RequestPacket<?> requestPacket = GsonPacketParser.fromJson(jsonLine);

                // Dòng này cũng sẽ lập tức XANH trở lại
                RequestRouter.getInstance().route(requestPacket, this);
            }
        } catch (Exception e) { // <--- QUAN TRỌNG: Đổi IOException thành Exception ở đây
            System.err.println("Lỗi kết nối hoặc lỗi dịch JSON từ Client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // ... code đóng socket của bạn ...
        }
    }

    /**
     * Gửi ResponsePacket về client (NDJSON: mỗi packet một dòng)
     */
    public synchronized void sendPacket(ResponsePacket<?> packet) {
        if (out == null) return;
        String jsonStr = gson.toJson(packet);
        out.println(jsonStr);
    }

    /**
     * Đóng các luồng và socket an toàn
     */
    public void forceClose() {
        try {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wrapper để AuctionServer gọi khi kickUser
     */
    public void closeConnection() {
        forceClose();
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Khi handler biết userId (ví dụ sau login thành công), gọi setUserId và đăng ký vào server
     */
    public void setUserId(String userId) {
        this.userId = userId;
        if (userId != null) {
            server.registerClient(userId, this);
        }
    }
}