package com.uet.BiddingApplication.Utils;

import com.google.gson.*;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;

import java.lang.reflect.Type;
import java.time.Instant;

/**
 * Lõi phân tích gói tin mạng.
 * Xử lý ngoại lệ chặt chẽ, chống crash hệ thống khi nhận chuỗi JSON rác.
 */
public class GsonPacketParser {
    // Chỉ khởi tạo 1 lần duy nhất, thread-safe
    private static final Gson gson = new GsonBuilder().create();

    public static String serialize(Object packet) {
        return gson.toJson(packet) + "\n"; // Chuẩn NDJSON
    }

    /**
     * Dùng cho Server khi nhận Request từ Client
     */
    public static RequestPacket<?> deserializeRequest(String jsonLine) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonLine).getAsJsonObject();
            ActionType action = ActionType.valueOf(jsonObject.get("action").getAsString());

            // Xử lý bảo mật: Chặn các request quá hạn (Replay Attack / Lag)
            long timestamp = jsonObject.has("timestamp") ? jsonObject.get("timestamp").getAsLong() : Instant.now().toEpochMilli();

            RequestPacket<Object> packet = new RequestPacket<>();
            packet.setAction(action);
            packet.setToken(jsonObject.has("token") ? jsonObject.get("token").getAsString() : null);
            packet.setTimestamp(timestamp);

            if (jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                // Tra cứu từ Registry thay vì hardcode switch-case ở đây
                Type payloadType = PacketTypeRegistry.getRequestType(action);
                Object payload = gson.fromJson(jsonObject.get("payload"), payloadType);
                packet.setPayload(payload);
            }
            return packet;

        } catch (JsonSyntaxException | IllegalArgumentException e) {
            // Log lỗi nghiêm trọng để truy vết, chặn các JSON malformed tấn công Server
            System.err.println("[SECURITY/PARSE ERROR] Invalid packet format: " + e.getMessage());
            return null;
        }
    }

    /**
     * Dùng cho Client khi nhận Response từ Server (Logic tương tự)
     */
    public static ResponsePacket<?> deserializeResponse(String jsonLine) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonLine).getAsJsonObject();
            ActionType action = ActionType.valueOf(jsonObject.get("action").getAsString());
            int statusCode = jsonObject.get("statusCode").getAsInt();

            ResponsePacket<Object> packet = new ResponsePacket<>();
            packet.setAction(action);
            packet.setStatusCode(statusCode);

            if (jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                Type payloadType = PacketTypeRegistry.getResponseType(action);
                Object payload = gson.fromJson(jsonObject.get("payload"), payloadType);
                packet.setPayload(payload);
            }
            return packet;

        } catch (Exception e) {
            System.err.println("[CLIENT PARSE ERROR] Corrupted response: " + e.getMessage());
            return null;
        }
    }
}
