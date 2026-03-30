package com.uet.BiddingApplication.Server_class;

import com.uet.BiddingApplication.DTO.Packet.RequestPacket;

public class RequestRouter {
    public void route(RequestPacket<?> request, ClientConnectionHandler handler){
        /*
        	Phân tích ActionType.
        	Kiểm tra token (trừ Login/Register). Nếu token sai, gửi trả ResponsePacket lỗi 401 ngay lập tức.
        	Gọi các Service của Thành viên 3 (AuthService, AuctionService, v.v.).
        	Nhận kết quả từ Service và gọi handler.sendPacket(response).
*/
    }
}
