package com.uet.BiddingApplication.Util; // Thường để trong thư mục Service hoặc Cache

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.AuctionCardDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Session.ResponseDispatcher;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RegisteredSessionUtil {

    // 1. Biến static lưu trữ thể hiện duy nhất (Cái này chính là cơ chế tự lưu cache)
    private static RegisteredSessionUtil instance;

    // 2. Tập hợp chứa danh sách các ID đã đăng ký
    private final Set<String> registeredSessionIds = ConcurrentHashMap.newKeySet();

    // 3. Constructor để PRIVATE: Ngăn cấm mọi Controller gọi lệnh 'new'
    private RegisteredSessionUtil() {
        // Tự động lắng nghe kết quả từ server ngay khi class này ra đời
        ResponseDispatcher.getInstance().subscribe(ActionType.GET_REGISTERED_SESSIONS, this::handleResponse);
    }

    // 4. Hàm lấy instance toàn cục
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
}