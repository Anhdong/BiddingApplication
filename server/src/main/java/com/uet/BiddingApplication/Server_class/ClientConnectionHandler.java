package com.uet.BiddingApplication.Server_class;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionHandler implements Runnable {

    private Socket socket;

    // V2.0: Dùng luồng Text thay cho ObjectStream
    private BufferedReader in;
    private PrintWriter out;

    private String userId;

    public ClientConnectionHandler(Socket socket) {
        // TODO: Gán socket và khởi tạo các luồng 'in' (BufferedReader) và 'out' (PrintWriter có autoFlush)
    }

    @Override
    public void run() {
        // TODO: Dùng vòng lặp while để liên tục đọc từng dòng JSON từ 'in'.
        // Sau đó dịch sang RequestPacket và đẩy cho RequestRouter xử lý.
        // Nhớ xử lý dọn dẹp khi Client ngắt kết nối (catch IOException).
    }

    public void sendPacket(ResponsePacket<?> packet) {
        // TODO: Dịch ResponsePacket thành chuỗi JSON.
        // Gửi đi bằng out.println() để đảm bảo có ký tự \n ở cuối (chuẩn NDJSON).
    }

    public void forceClose() {
        // TODO: Đóng các luồng (in, out) và đóng socket một cách an toàn.
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}