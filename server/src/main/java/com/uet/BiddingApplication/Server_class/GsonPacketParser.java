package com.uet.BiddingApplication.Server_class;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.Enum.ActionType;
// Import các DTO request (bạn hãy tự import cho đủ nhé)
import com.uet.BiddingApplication.DTO.Request.*;

import java.lang.reflect.Type;

public class GsonPacketParser {

    private static final Gson gson = new Gson();

    public static RequestPacket<?> fromJson(String jsonString) throws Exception {
        // Bước 1: Đọc nháp JSON để lấy ra ActionType
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        String actionStr = jsonObject.get("action").getAsString();
        ActionType action = ActionType.valueOf(actionStr);

        // Bước 2: Dựa vào ActionType, quyết định class của Payload
        Class<?> payloadClass;

        switch (action) {
            case LOGIN:
                payloadClass = AuthRequestDTO.class;
                break;
            case REGISTER:
                payloadClass = RegisterRequestDTO.class;
                break;
            case PLACE_MANUAL_BID:
                payloadClass = BidRequestDTO.class;
                break;
            case REGISTER_AUTO_BID:
                payloadClass = AutoBidRegisterDTO.class;
                break;
            case CREATE_ITEM:
                payloadClass = AutoBidRegisterDTO.class; // Hoặc ItemCreateDTO tùy tên file của bạn
                break;
            case UPDATE_PROFILE:
                payloadClass = ProfileUpdateRequestDTO.class;
                break;
            case PRE_REGISTER_SESSION:
                payloadClass = SessionRegisterRequestDTO.class;
                break;
            // Thêm các case khác tương ứng với file Request của bạn...
            default:
                // Nếu Request không cần payload (chỉ có action và token)
                payloadClass = String.class;
                break;
        }

        // Bước 3: Ép kiểu chính xác bằng TypeToken của Gson
        Type type = TypeToken.getParameterized(RequestPacket.class, payloadClass).getType();
        return gson.fromJson(jsonString, type);
    }

    // Hàm tiện ích để dịch ResponsePacket thành chuỗi JSON (dùng khi gửi đi)
    public static String toJson(Object packet) {
        return gson.toJson(packet);
    }
}