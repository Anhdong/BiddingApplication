package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.DTO.Request.*; // Import các DTO để cast dữ liệu

import java.util.EnumMap;
import java.util.Map;
//Lưu trữ danh sách các hành động (Action)
// -> Gán đúng code xử lý (Handler) tương ứng cho từng hành động đó
// -> Trực tiếp thực hiện các thao tác nghiệp vụ
// -> Trả về kết quả cuối cùng cho Router.
/**
 * File này đóng vai trò là bảng điều phối logic phía Server.
 */
public class RouteRegistry {

    // Giao diện để thực thi logic
    public interface CommandHandler {
        ResponsePacket<?> handle(RequestPacket<?> request, ClientConnectionHandler client);
    }

    private static final Map<ActionType, CommandHandler> registry = new EnumMap<>(ActionType.class);

    static {
        // --- NHÓM HỆ THỐNG & TÀI KHOẢN ---

        registry.put(ActionType.LOGIN, (req, client) -> {
            // Nhờ GsonPacketParser đã parse đúng Type ở Common, ta chỉ việc cast
            AuthRequestDTO loginData = (AuthRequestDTO) req.getPayload();

            // Ở đây Thành viên 3 sẽ gọi: return AuthService.getInstance().login(loginData, client);
            System.out.println("[Logic] Đang xử lý Login cho: " + loginData.getUsername());
            return new ResponsePacket<>(ActionType.LOGIN, 200, "Đăng nhập thành công", null);
        });

        registry.put(ActionType.REGISTER, (req, client) -> {
            RegisterRequestDTO regData = (RegisterRequestDTO) req.getPayload();
            // Xử lý đăng ký...
            return new ResponsePacket<>(ActionType.REGISTER, 200, "Đăng ký tài khoản mới thành công", null);
        });

        // --- NHÓM PHÒNG ĐẤU GIÁ ---

        registry.put(ActionType.CREATE_ITEM, (req, client) -> {
            ItemCreateDTO itemData = (ItemCreateDTO) req.getPayload();
            // Logic tạo vật phẩm...
            return new ResponsePacket<>(ActionType.CREATE_ITEM, 200, "Vật phẩm đã được niêm yết", null);
        });

        registry.put(ActionType.PLACE_MANUAL_BID, (req, client) -> {
            BidRequestDTO bidData = (BidRequestDTO) req.getPayload();
            // Logic xử lý đặt giá, kiểm tra số dư...
            return new ResponsePacket<>(ActionType.PLACE_MANUAL_BID, 200, "Đặt giá thành công", null);
        });

        // --- CÁC ACTION KHÁC ---
        // Thêm các action như LOGOUT, GET_ALL_SESSIONS tương tự...
    }

    /**
     * Lấy Handler tương ứng với hành động.
     * Nếu không tìm thấy, trả về một Handler mặc định báo lỗi.
     */
    public static CommandHandler getHandler(ActionType action) {
        return registry.getOrDefault(action, (req, client) -> {
            System.err.println("[Route Error] Chưa có Handler cho hành động: " + action);
            return new ResponsePacket<>(action, 404, "Hành động chưa được hỗ trợ trên Server", null);
        });
    }
}