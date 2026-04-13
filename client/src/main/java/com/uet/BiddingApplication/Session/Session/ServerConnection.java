package com.uet.BiddingApplication.Session.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

    private static volatile ServerConnection instance;

    // Sử dụng Gson để chuyển đổi Object <-> JSON
    private final Gson gson = new Gson();

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
        try {
            // 1. Mở kết nối Socket tới Server
            socket = new Socket(host, port);

            // 2. Khởi tạo luồng Đọc/Ghi (Sử dụng chuẩn văn bản)
            // true ở đây có nghĩa là autoFlush - cứ gọi println là tự đẩy data đi ngay
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            System.out.println("[Client] Đã kết nối thành công tới Server: " + host + ":" + port);

            // 3. Vừa kết nối xong là phải mở luồng lắng nghe ngầm ngay lập tức
            listen();

        } catch (Exception e) {
            System.err.println("[Client] Lỗi kết nối tới Server: " + e.getMessage());
        }
    }

    public void sendRequest(RequestPacket<?> packet){
        if (out != null) {
            try {
                // Bước 1: Dịch gói tin RequestPacket thành chuỗi JSON
                String jsonStr = gson.toJson(packet);

                // Bước 2: Gửi đi bằng println() (Hàm này tự động nối thêm dấu \n ở cuối)
                out.println(jsonStr);

                System.out.println("[Client -> Server] Đã gửi: " + jsonStr);
            } catch (Exception e) {
                System.err.println("[Client] Lỗi khi gửi request: " + e.getMessage());
            }
        } else {
            System.err.println("[Client] Lỗi: Chưa kết nối tới Server nên không thể gửi!");
        }
    }

    public void listen(){
        listenerThread = new Thread(() -> {
            try {
                String jsonLine;
                // Vòng lặp while chặn (block) liên tục để hứng từng dòng JSON từ Server
                while ((jsonLine = in.readLine()) != null) {
                    System.out.println("[Server -> Client] Nhận được: " + jsonLine);

                    // TODO: Xử lý dữ liệu nhận được (Cập nhật lên giao diện)
                    // Ở đây bạn có thể bóc tách JSON ra để xem Server phản hồi gì
                    // Ví dụ:
                    /*
                    JsonObject jsonObject = JsonParser.parseString(jsonLine).getAsJsonObject();
                    String action = jsonObject.get("action").getAsString();

                    if (action.equals("REALTIME_PRICE_UPDATE")) {
                         // Lấy giá mới ra và cập nhật lên màn hình (JavaFX/Swing)
                    } else if (action.equals("SUCCESS")) {
                         // Báo popup "Đặt giá thành công"
                    }
                    */
                }
            } catch (Exception e) {
                System.out.println("[Client] Mất kết nối tới Server hoặc đã chủ động ngắt.");
            }
        });

        // Đặt Daemon = true để khi người dùng bấm dấu X tắt app, luồng này tự động chết theo
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void disconnect(){
        try {
            // Dừng luồng lắng nghe
            if (listenerThread != null) {
                listenerThread.interrupt();
            }

            // Đóng các ống nước và cắt đứt Socket
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("[Client] Đã ngắt kết nối an toàn.");
        } catch (Exception e) {
            System.err.println("[Client] Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }
}