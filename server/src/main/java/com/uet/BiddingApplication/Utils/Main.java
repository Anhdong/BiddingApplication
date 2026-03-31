package com.uet.BiddingApplication.Utils;

import com.uet.BiddingApplication.Utils.StorageService;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        System.out.println("⏳ Đang khởi tạo kết nối đọc cấu hình .env...");

        try {
            Connection connection = DatabaseConnectionPool.getConnection();
            // 1. Khởi tạo StorageService (Sẽ tự động nạp Supabase URL và Key từ .env)
            StorageService storageService = StorageService.getInstance();

            // 2. Tạo một nội dung giả để upload.
            // Dùng định dạng .txt thay vì ảnh để file rất nhẹ và dễ dàng kiểm tra trên trình duyệt
            String testContent = "Hello Supabase! Đây là tin nhắn test từ hệ thống Bidding Application.";
            byte[] fileBytes = testContent.getBytes();
            String extension = ".txt";

            System.out.println("🚀 Đang gửi yêu cầu đẩy file lên Supabase Storage...");

            // 3. Thực hiện gọi API thật
            String publicUrl = storageService.uploadImage(fileBytes, extension);

            // 4. In kết quả thành công
            System.out.println("\n✅ [KẾT QUẢ THÀNH CÔNG] - Kết nối Supabase hoạt động hoàn hảo!");
            System.out.println("🔗 Link public của file (bạn có thể click vào xem thử):");
            System.out.println(publicUrl);

        } catch (Exception e) {
            // Xử lý và in ra lỗi nếu sai Key, sai tên Bucket, hoặc mất mạng
            System.err.println("\n❌ [KẾT QUẢ THẤT BẠI] - Có lỗi trong quá trình kết nối hoặc Upload:");
            e.printStackTrace();
            System.err.println("\n💡 Gợi ý kiểm tra:");
            System.err.println("1. File .env đã nằm đúng thư mục gốc chưa?");
            System.err.println("2. Biến SUPABASE_URL, SUPABASE_SERVICE_KEY, SUPABASE_BUCKET_NAME có chính xác không?");
            System.err.println("3. Tên Bucket có tồn tại trên Supabase và có set quyền Public chưa?");
        }
    }
}