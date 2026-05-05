package com.uet.BiddingApplication.Session;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.Utils.GsonPacketParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

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

    // =========================================================================
    // FIX 1: Thêm hàm Static để ClientMain gọi được: ServerConnection.fromRemoteConfig()
    // =========================================================================
    public static ServerConnection fromRemoteConfig() throws Exception {
        ServerConnection conn = getInstance();
        conn.connectFromRemoteConfig();
        return conn;
    }

    public void connectFromRemoteConfig() throws Exception {
        // FIX 2: Dùng đúng hàm fetch() từ class Remoteconfigfetcher của bạn
        Remoteconfigfetcher.ServerAddress addr = Remoteconfigfetcher.fetch();
        connect(addr.host, addr.port);
    }

    public void connect(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        this.listenerThread = new ResponseListenerThread(in);
        this.threadHandle = new Thread(listenerThread);
        this.threadHandle.setDaemon(true);
        this.threadHandle.start();
        System.out.println("[Connection] Kết nối thành công tới " + host + ":" + port);
    }

    public void disconnect() {
        try {
            if (listenerThread != null) listenerThread.stopListening();
            if (threadHandle != null) threadHandle.interrupt();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            System.err.println("Lỗi ngắt kết nối: " + e.getMessage());
        }
    }

    public void sendRequest(RequestPacket<?> request) {
        if (out != null) {
            try {
                out.println(GsonPacketParser.serialize(request));
            } catch (Exception e) {
                System.err.println("Lỗi gửi dữ liệu: " + e.getMessage());
            }
        }
    }
}