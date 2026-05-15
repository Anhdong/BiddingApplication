package com.uet.BiddingApplication.Utils;

import com.google.gson.*;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lõi phân tích gói tin mạng.
 * Xử lý ngoại lệ chặt chẽ, chống crash hệ thống khi nhận chuỗi JSON rác.
 */
public class GsonPacketParser {
    // TỐI ƯU: Cấu hình GsonBuilder với TypeAdapter cho LocalDateTime
    private static final Gson gson = new GsonBuilder()
            // Dạy Gson cách Serialize: LocalDateTime -> String JSON
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))

            // Dạy Gson cách Deserialize: String JSON -> LocalDateTime
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))

            // (Tùy chọn) Có thể format ngày giờ cho đẹp hơn nếu cần
            .create();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GsonPacketParser.class);
    public static String serialize(Object packet) {
        return gson.toJson(packet); // Chuẩn NDJSON
    }
    //chuyển đối tượng -> JSON , kết thúc lệnh bằng dấu /n
    /**
     * Dùng cho Server khi nhận Request từ Client
     */
    public static RequestPacket<?> deserializeRequest(String jsonLine) {
        if (jsonLine == null || jsonLine.trim().isEmpty()) {
            return null;
        }
        //đoch chuổix Json từ client gửi , thực hiện các thao tác để trả về 1 requestpacket
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonLine).getAsJsonObject();
            ActionType action = ActionType.valueOf(jsonObject.get("action").getAsString());

            // Chặn các request quá hạn
            long timestamp = jsonObject.has("timestamp") ? jsonObject.get("timestamp").getAsLong() : Instant.now().toEpochMilli();

            RequestPacket<Object> packet = new RequestPacket<>();
            packet.setAction(action);
            packet.setUserId(jsonObject.has("userId") ? jsonObject.get("userId").getAsString() : null);
            packet.setToken(jsonObject.has("token") ? jsonObject.get("token").getAsString() : null);
            packet.setTimestamp(timestamp);

            if (jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                // Tra cứu từ Registry thay vì hardcode switch-case ở đây
                Type payloadType = PacketTypeRegistry.getRequestType(action);
                // Nếu hệ thống định nghĩa Action này không có data,
                // ta phớt lờ luôn payload Client gửi lên.
                if (payloadType != Void.class && jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                    Object payload = gson.fromJson(jsonObject.get("payload"), payloadType);
                    packet.setPayload(payload);
                }
            }
            return packet;

        } catch (JsonSyntaxException | IllegalArgumentException e) {
            // Log lỗi nghiêm trọng để truy vết, chặn các JSON malformed tấn công Server
            log.error("[SECURITY/PARSE ERROR] Invalid packet format: " + e.getMessage());
            return null;
        }
    }

    /**
     * Dùng cho Client khi nhận Response từ Server (Logic tương tự)
     */
    public static ResponsePacket<?> deserializeResponse(String jsonLine) {
        if (jsonLine == null || jsonLine.trim().isEmpty()) {
            return null;
        }
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonLine).getAsJsonObject();
            ActionType action = ActionType.valueOf(jsonObject.get("action").getAsString());
            int statusCode = jsonObject.get("statusCode").getAsInt();

            ResponsePacket<Object> packet = new ResponsePacket<>();
            packet.setAction(action);
            packet.setStatusCode(statusCode);
            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                packet.setMessage(jsonObject.get("message").getAsString());
            }

            if (jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                Type payloadType = PacketTypeRegistry.getResponseType(action);
                // Nếu hệ thống định nghĩa Action này không có data,
                // ta phớt lờ luôn payload Client gửi lên.
                if (payloadType != Void.class && jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                    Object payload = gson.fromJson(jsonObject.get("payload"), payloadType);
                    packet.setPayload(payload);
                }
            }
            return packet;

        } catch (Exception e) {
            log.error("[CLIENT PARSE ERROR] Corrupted response: " + e.getMessage());
            return null;
        }
    }
}
