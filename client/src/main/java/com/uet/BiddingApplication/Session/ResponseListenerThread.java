package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Utils.GsonPacketParser;
import java.io.BufferedReader;

public class ResponseListenerThread implements Runnable {

    private final BufferedReader in;
    // Dùng volatile để đảm bảo khi gọi stopListening(), luồng sẽ dừng ngay lập tức một cách an toàn
    private volatile boolean running = true;

    public ResponseListenerThread(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String jsonLine;
            // Vòng lặp chặn (block) liên tục hứng dòng JSON từ Server
            while (running && (jsonLine = in.readLine()) != null) {
                System.out.println("[Server -> Client] Nhận được: " + jsonLine);

                // Dịch chuỗi JSON thành Đối tượng ResponsePacket
                ResponsePacket<?> response = GsonPacketParser.deserializeResponse(jsonLine);

                if (response != null) {
                    // Đẩy gói tin sang cho "Điều phối viên" xử lý
                    ResponseDispatcher.getInstance().dispatch(response);
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[Client] Mất kết nối tới Server hoặc lỗi luồng nghe: " + e.getMessage());
            } else {
                System.out.println("[Client] Luồng lắng nghe đã được đóng chủ động.");
            }
        }
    }

    // Hàm này được gọi từ ServerConnection khi muốn ngắt kết nối
    public void stopListening() {
        this.running = false;
    }
}