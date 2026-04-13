package com.uet.BiddingApplication.Server_class;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO; // Nhớ import đúng tên file của bạn

public class RequestRouter {

    private static volatile RequestRouter instance;
    private RequestRouter() {}

    public static RequestRouter getInstance(){
        if(instance == null ){
            synchronized (RequestRouter.class){
                if(instance == null){
                    instance = new RequestRouter();
                }
            }
        }
        return instance;
    }

    // ĐÂY LÀ HÀM BỊ THIẾU KHIẾN BẠN BỊ BÁO ĐỎ:
    public void route(RequestPacket<?> request, ClientConnectionHandler handler) {
        try {
            ActionType action = request.getAction();
            System.out.println("[Router] Nhận yêu cầu: " + action);

            // 1. Kiểm tra Token (bỏ qua Login/Register)
            if (action != ActionType.LOGIN && action != ActionType.REGISTER) {
                boolean isTokenValid = true; // TODO: Cần check qua SessionManager
                if (!isTokenValid) {
                    handler.sendPacket(new ResponsePacket<>(action, 401, "Token không hợp lệ!", null));
                    return;
                }
            }

            // 2. Chặn lệnh đặt giá (Không đợi phản hồi)
            if (action == ActionType.PLACE_MANUAL_BID) {
                // InMemoryBidServiceImpl.getInstance().enqueueBid(...);
                return;
            }

            // 3. Gọi RouteRegistry xử lý
            RouteRegistry.CommandHandler command = RouteRegistry.getHandler(action);
            if (command != null) {
                ResponsePacket<?> response = command.handle(request, handler);
                if (response != null) {
                    handler.sendPacket(response);
                }
            } else {
                handler.sendPacket(new ResponsePacket<>(action, 400, "Lệnh chưa hỗ trợ!", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}