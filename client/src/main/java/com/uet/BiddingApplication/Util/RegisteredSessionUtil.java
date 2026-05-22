package com.uet.BiddingApplication.Util; // Thường để trong thư mục Service hoặc Cache

import com.uet.BiddingApplication.Controller.BidderController.BidderWatchlistController;
import com.uet.BiddingApplication.DTO.Packet.RequestPacket;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Session.ClientSession;
import com.uet.BiddingApplication.Session.ResponseDispatcher;
import com.uet.BiddingApplication.Session.ServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RegisteredSessionUtil {

    private static final Logger log = LoggerFactory.getLogger(RegisteredSessionUtil.class);


    private static RegisteredSessionUtil instance;

    private final Set<String> registeredSessionIds = ConcurrentHashMap.newKeySet();

    private RegisteredSessionUtil() {
        // Tự động lắng nghe kết quả từ server ngay khi class này ra đời
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_REGISTERED_SESSIONS, this::handleResponse);
    }

    public static RegisteredSessionUtil getInstance() {
        if (instance == null) {
            instance = new RegisteredSessionUtil();
        }
        return instance;
    }

    // --- XỬ LÝ LOGIC ---

    // Hàm nhận dữ liệu từ mạng và cập nhật kho
    private void handleResponse(ResponsePacket<?> response) {
        if (response.getStatusCode() == 200) {
            registeredSessionIds.clear();
            List<AuctionCardDTO> payload = (List<AuctionCardDTO>) response.getPayload();
            if (payload != null) {
                for (AuctionCardDTO dto : payload) {
                    registeredSessionIds.add((dto.getSessionId()));
                }
            }
        }
    }

    // Các hàm công khai cho các Controller khác mượn dùng:

    public boolean isRegistered(String sessionId) {
        return registeredSessionIds.contains(sessionId);
    }

    public void addSession(String sessionId) {
        registeredSessionIds.add(sessionId);
    }

    public Set<String> getAllRegisteredIds() {
        return registeredSessionIds; // Trả về để Tab Browse dùng
    }

    public void requestRegisteredSessions(){
        log.info("[RegisteredSessionUtil] Đang gửi yêu cầu lấy danh sách đấu giá đã đăng ký...");
        RequestPacket<Void> request = new RequestPacket<>();
        request.setAction(ActionType.GET_REGISTERED_SESSIONS);
        request.setUserId(ClientSession.getInstance().getCurrentUser().getId());
        request.setToken(ClientSession.getInstance().getCurrentToken());
        ServerConnection.getInstance().sendRequest(request);
    }
}