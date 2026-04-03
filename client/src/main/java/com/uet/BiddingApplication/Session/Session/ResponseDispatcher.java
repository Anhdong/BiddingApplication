package com.uet.BiddingApplication.Session.Session;

import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;

public class ResponseDispatcher {

    // 1. Sửa thành private để bảo vệ an toàn
    private static volatile ResponseDispatcher instance;

    private ResponseDispatcher() {}

    public static ResponseDispatcher getInstance(){
        if(instance == null ){
            // 2. Đã sửa lỗi: Khóa đúng ResponseDispatcher.class
            synchronized (ResponseDispatcher.class){
                if(instance == null){
                    instance = new ResponseDispatcher();
                }
            }
        }
        return instance;
    }

    // Phương thức này giữ nguyên cấu trúc
    public void dispatch(ResponsePacket<?> response){
        /* LƯU Ý V2.0:
         * Object 'response' ở đây là do GsonPacketParser vừa dịch từ chuỗi JSON (NDJSON) ra.
         * Class này chỉ việc lấy dữ liệu và gọi Platform.runLater() để vẽ lên màn hình.
         */

        // switch(response.getAction()) {
        //     case LOGIN_SUCCESS:
        //          Platform.runLater(() -> { ... });
        //          break;
        // }
    }
}