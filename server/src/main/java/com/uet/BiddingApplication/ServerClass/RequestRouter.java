package com.uet.BiddingApplication.ServerClass;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.Enum.ActionType;
// Import BusinessException của bạn vào đây (nếu khác package)
import com.uet.BiddingApplication.Exception.BusinessException;

public class RequestRouter {

    private static volatile RequestRouter instance;

    private RequestRouter() {
    }

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
     * Hàm điều hướng và Lưới hứng lỗi trung tâm của Server
     */
    public void route(RequestPacket<?> request, ClientConnectionHandler client) {
        ActionType currentAction = null;

        try {
            if (request == null) {
                // Ném thẳng lỗi kèm lý do (chỉ dùng String)
                throw new BusinessException("Gói tin yêu cầu không hợp lệ (Null Packet)");
            }

            currentAction = request.getAction();

            if (currentAction == null) {
                throw new BusinessException("Gói tin thiếu trường ActionType");
            }

            // 1. Lấy Handler tương ứng từ RouteRegistry
            RouteRegistry.CommandHandler command = RouteRegistry.getHandler(currentAction);

            // 2. Thực thi logic nghiệp vụ
            ResponsePacket<?> response = command.handle(request);

            // 3. Nếu xử lý thành công, gửi trả ResponsePacket về Client
            if (response != null) {
                client.sendPacket(response);
            }

        } catch (BusinessException e) {
            // =========================================================
            // CHUẨN YÊU CẦU: Đóng gói ResponsePacket với statusCode = 400,
            // kèm message do Service ném ra.
            // =========================================================
            System.out.println("[Router - Business] Từ chối yêu cầu " + currentAction + " của " +
                    (client.getUserId() != null ? client.getUserId() : "Guest") + ": " + e.getMessage());

            // Trực tiếp gán statusCode 400 và truyền e.getMessage()
            ResponsePacket<Void> errorResponse = new ResponsePacket<>(currentAction, 400, e.getMessage(), null);
            client.sendPacket(errorResponse);

        } catch (Exception e) {
            // =========================================================
            // CHUẨN YÊU CẦU: Bắt các lỗi hệ thống (NullPointer, rớt DB),
            // đóng gói status 500, báo "Lỗi máy chủ nội bộ" và ghi log console.
            // =========================================================
            System.err.println("[Router - System Error] Lỗi nghiêm trọng khi xử lý " + currentAction +
                    " từ " + (client.getUserId() != null ? client.getUserId() : "Guest"));
            e.printStackTrace(); // Ghi log console

            // Trực tiếp gán statusCode 500
            ResponsePacket<Void> serverErrorResponse = new ResponsePacket<>(currentAction, 500, "Lỗi máy chủ nội bộ", null);
            client.sendPacket(serverErrorResponse);
        }
    }
}