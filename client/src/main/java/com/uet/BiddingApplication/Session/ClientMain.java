package com.uet.BiddingApplication.Session;

// Import class BiddingApplication nằm ở package com.uet.BiddingApplication
import com.uet.BiddingApplication.BiddingApplication;

public class ClientMain {

    public static void main(String[] args) {
        ServerConnection connection = null;
        try {
            // Bây giờ hàm này đã tồn tại dưới dạng static trong ServerConnection
            connection = ServerConnection.fromRemoteConfig();

            // Khởi động giao diện/logic chính của App
            // Lưu ý: Đảm bảo class BiddingApplication của bạn có constructor nhận (ServerConnection)
        } catch (java.net.UnknownHostException e) {
            showError("Không tìm thấy host. Kiểm tra lại địa chỉ trong Gist.");
        } catch (java.net.ConnectException e) {
            showError("Kết nối bị từ chối. Server hoặc Ngrok có thể chưa bật.");
        } catch (Exception e) {
            showError("Lỗi hệ thống: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Đóng kết nối nếu có lỗi xảy ra hoặc khi tắt app
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void showError(String message) {
        System.err.println("[ClientMain] " + message);
    }
}