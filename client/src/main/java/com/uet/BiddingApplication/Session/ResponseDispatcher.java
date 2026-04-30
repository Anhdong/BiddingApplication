package com.uet.BiddingApplication.Session;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ResponseDispatcher {

    private static volatile ResponseDispatcher instance;

    // Bản đồ lưu trữ Callback của các UI Controllers (Group 2)
    private final Map<ActionType, List<Consumer<ResponsePacket<?>>>> listeners = new ConcurrentHashMap<>();

    private ResponseDispatcher() {}

    public static ResponseDispatcher getInstance() {
        if (instance == null) {
            synchronized (ResponseDispatcher.class) {
                if (instance == null) {
                    instance = new ResponseDispatcher();
                }
            }
        }
        return instance;
    }

    public void subscribe(ActionType action, Consumer<ResponsePacket<?>> callback) {
        listeners.computeIfAbsent(action, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void unsubscribe(ActionType action, Consumer<ResponsePacket<?>> callback) {
        List<Consumer<ResponsePacket<?>>> actionListeners = listeners.get(action);
        if (actionListeners != null) {
            actionListeners.remove(callback);
        }
    }

    // Hàm dispatch chuẩn theo sơ đồ Group 1
    public void dispatch(ResponsePacket<?> response) {
        List<Consumer<ResponsePacket<?>>> actionListeners = listeners.get(response.getAction());

        if (actionListeners != null && !actionListeners.isEmpty()) {
            // Đẩy sang UI Thread an toàn
            Platform.runLater(() -> {
                for (Consumer<ResponsePacket<?>> callback : actionListeners) {
                    try {
                        callback.accept(response);
                    } catch (Exception e) {
                        System.err.println("[Dispatcher] Lỗi khi Controller xử lý UI: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } else {
            // Log nhẹ nhàng nếu Client nhận được tin nhắn rác hoặc chưa kịp mở màn hình
            System.out.println("[Dispatcher] Tin nhắn bị bỏ qua do chưa có UI lắng nghe: " + response.getAction());
        }
    }
}