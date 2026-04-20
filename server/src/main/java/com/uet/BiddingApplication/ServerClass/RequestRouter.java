package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
//Tiếp nhận yêu cầu từ Handler -> Kiểm tra bảo mật (Token)
// -> Tra cứu bản đồ RouteRegistry để tìm đúng phòng ban xử lý
// -> Thực thi code logic -> Nhận kết quả và giao lại cho Handler gửi về.
// Đồng thời, nó đóng vai trò chốt chặn bắt lỗi tổng, đảm bảo Server không bao giờ bị sập khi luồng xử lý gặp sự cố
/**
 * Front Controller: Tiếp nhận mọi yêu cầu từ Handler,
 * kiểm tra bảo mật cơ bản và điều hướng sang RouteRegistry.
 */
public class RequestRouter {

    private static volatile RequestRouter instance;

    private RequestRouter() {}

    public static RequestRouter getInstance() {
        if (instance == null) {
            synchronized (RequestRouter.class) {
                if (instance == null) {
                    instance = new RequestRouter();
                }
            }
        }
        return instance;
    }

    /**
     * Hàm điều phối chính
     */
    public void route(RequestPacket<?> request, ClientConnectionHandler handler) {
        if (request == null || request.getAction() == null) {
            System.err.println("[Router] Nhận được gói tin rác hoặc Action bị null.");
            return;
        }

        ActionType action = request.getAction();
        System.out.println("[Router] Đang xử lý lệnh: " + action + " cho User: " + handler.getUserId());

        try {
            // 1. KIỂM TRA BẢO MẬT (TOKEN)
            // Chỉ kiểm tra Token nếu không phải là Login hoặc Register
            if (action != ActionType.LOGIN && action != ActionType.REGISTER) {
                if (!isValidToken(request.getToken())) {
                    handler.sendPacket(new ResponsePacket<>(action, 401, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!", null));
                    return;
                }
            }

            // 2. LẤY HANDLER TỪ REGISTRY
            // Thay vì dùng if-else cho PLACE_MANUAL_BID ở đây,
            // ta nên để RouteRegistry quản lý hết cho sạch code.
            RouteRegistry.CommandHandler command = RouteRegistry.getHandler(action);

            // 3. THỰC THI LOGIC
            ResponsePacket<?> response = command.handle(request);

            // 4. GỬI PHẢN HỒI (Nếu có)
            if (response != null) {
                handler.sendPacket(response);
            }

        } catch (Exception e) {
            System.err.println("[Router Error] Lỗi nghiêm trọng khi điều hướng Action " + action + ": " + e.getMessage());
            e.printStackTrace();
            // Báo lỗi về cho Client để tránh treo UI
            handler.sendPacket(new ResponsePacket<>(action, 500, "Lỗi hệ thống phía Server!", null));
        }
    }

    /**
     * Kiểm tra tính hợp lệ của Token
     * (Sau này Thành viên 3 sẽ tích hợp SessionManager vào đây)
     */
    private boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) return false;
        // Tạm thời chấp nhận mọi token không null để test.
        // Sau này sẽ check trong Map<String, User> của Server.
        return true;
    }
}