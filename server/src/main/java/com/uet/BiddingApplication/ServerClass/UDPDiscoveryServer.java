package com.uet.BiddingApplication.ServerClass;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPDiscoveryServer implements Runnable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UDPDiscoveryServer.class);
    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(8888)) {
            log.info("Bật luồng phản hồi UDP Discovery trên Port 8888...");
            byte[] receiveBuffer = new byte[1024];

            // Vòng lặp an toàn, tự dừng nếu Thread bị ngắt (interrupt)
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
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
        } catch (Exception e) {
            // Khi server gọi udpDiscoveryThread.interrupt() hoặc tắt JVM, nó sẽ rơi vào đây
            log.info("Luồng UDP Discovery đã được đóng.");
        }
    }
}