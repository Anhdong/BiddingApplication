package com.uet.BiddingApplication.Config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Class quản lý cấu hình tập trung.
 * Đọc dữ liệu từ file application.properties trong thư mục resources.
 */
public class AppConfig {

    private static final Properties properties = new Properties();
    private static boolean isLoaded = false;

    // Chặn khởi tạo đối tượng (Utility Class)
    private AppConfig() {}

    /**
     * Hàm này BẮT BUỘC phải được gọi đầu tiên trong hàm main() của ServerLauncher.
     */
    public static void loadConfig() {
        if (isLoaded) return;

        // Dùng ClassLoader để tìm file trong thư mục resources
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {

            if (input == null) {
                System.err.println("CRITICAL: Không tìm thấy file application.properties trong thư mục resources!");
                System.exit(1); // Dừng Server ngay lập tức vì không có cấu hình
            }

            // Nạp toàn bộ key-value vào bộ nhớ
            properties.load(input);
            isLoaded = true;
            System.out.println("Đã nạp thành công file cấu hình application.properties.");

        } catch (Exception e) {
            System.err.println("Lỗi khi đọc file cấu hình: " + e.getMessage());
            System.exit(1);
        }
    }

    // =========================================================
    // CÁC HÀM GETTER TIỆN ÍCH (Kèm theo giá trị mặc định Fallback)
    // =========================================================

    public static int getServerPort() {
        // Nếu trong file không ghi server.port, hệ thống sẽ tự dùng 8080 làm mặc định
        String portStr = properties.getProperty("server.port", "8080");
        return Integer.parseInt(portStr);
    }
    public static int getWorkerPoolSize() {
        String sizeStr = properties.getProperty("server.max.threads", "8");
        return Integer.parseInt(sizeStr);
    }
}