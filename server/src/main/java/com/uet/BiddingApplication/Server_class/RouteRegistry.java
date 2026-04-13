package com.uet.BiddingApplication.Server_class;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;

import java.util.EnumMap;
import java.util.Map;

public class RouteRegistry {

    // Khai báo một giao diện chức năng (Functional Interface)
    public interface CommandHandler {
        ResponsePacket<?> handle(RequestPacket<?> request, ClientConnectionHandler client);
    }

    // Cuốn sổ lưu trữ: ActionType đi với Hàm xử lý nào
    private static final Map<ActionType, CommandHandler> registry = new EnumMap<>(ActionType.class);

    static {
        // Đăng ký các luồng xử lý ở đây (Thành viên 3 sẽ gọi Service vào đây)

        registry.put(ActionType.LOGIN, (req, client) -> {
            // AuthRequestDTO loginData = (AuthRequestDTO) req.getPayload();
            // return AuthService.getInstance().login(loginData);
            return new ResponsePacket<>(req.getAction(), 200, "Login mock success", null);
        });

        registry.put(ActionType.REGISTER, (req, client) -> {
            return new ResponsePacket<>(req.getAction(), 200, "Register mock success", null);
        });

        registry.put(ActionType.CREATE_ITEM, (req, client) -> {
            return new ResponsePacket<>(req.getAction(), 200, "Tạo vật phẩm thành công", null);
        });

        // V.V... Các action khác thêm vào tương tự
    }

    // Hàm để Router gọi lấy Handler
    public static CommandHandler getHandler(ActionType action) {
        return registry.get(action);
    }
}