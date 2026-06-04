package com.uet.BiddingApplication.ServerClass;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPDiscoveryServer implements Runnable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UDPDiscoveryServer.class);

    // Đưa biến socket ra ngoài để có thể truy cập đóng mở từ hàm stop()
    private DatagramSocket socket;
    // Cờ báo hiệu trạng thái chạy
    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(8888);
            log.info("Bật luồng phản hồi UDP Discovery trên Port 8888...");
            byte[] receiveBuffer = new byte[1024];

            // Kết hợp cờ running và ngắt luồng
            while (running && !Thread.currentThread().isInterrupted()) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                // Hàm này đang bị chặn. Nếu socket.close() được gọi từ ngoài, nó sẽ ném SocketException
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                if ("WHERE_IS_AUCTION_SERVER".equals(message)) {
                    byte[] sendData = "I_AM_AUCTION_SERVER".getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length,
                            receivePacket.getAddress(),
                            receivePacket.getPort()
                    );
                    socket.send(sendPacket);
                }
            }
        } catch (SocketException e) {
            // Khi luồng Test gọi udpServer.stop(), chương trình sẽ lập tức nhảy vào đây
            log.info("Socket UDP đã bị đóng chủ động từ bên ngoài.");
        } catch (Exception e) {
            log.error("Lỗi trong luồng UDP Discovery:", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Cơ chế dừng an toàn: Gọi từ bên ngoài (luồng chính/Test) để đóng trực tiếp Socket.
     */
    public void stop() {
        this.running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Điểm chốt để giải phóng receive()
        }
    }

    private void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        log.info("Luồng UDP Discovery đã được giải phóng hoàn toàn.");
    }
}