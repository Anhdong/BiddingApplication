package com.uet.BiddingApplication.Utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public class StorageService {
    private static volatile StorageService instance;

    private final String Url;
    private final String Key;
    private final String bucketName;
    private final HttpClient httpClient;

    private StorageService() {
        //kiểm tra file .env nằm ở đâu
        String[] possiblePaths = {"./", "./server/", "../server/", "../"};
        String directory = "./"; // Mặc định
        boolean fileFound = false;

        for (String path : possiblePaths) {
            if (new File(path + ".env").exists()) {
                directory = path;
                fileFound = true;
                break; // Tìm thấy thì dừng quét ngay
            }
        }

        if (!fileFound) {
            System.err.println("⚠️ Cảnh báo: Không tìm thấy file .env ở bất kỳ thư mục dự kiến nào!");
        }
        //Đọc file .env
        Dotenv dotenv = Dotenv.configure().directory(directory).ignoreIfMissing().load();

        // Gán giá trị cho các biến final
        this.Url = dotenv.get("SUPABASE_URL");
        this.Key = dotenv.get("SUPABASE_SERVICE_KEY");
        this.bucketName = dotenv.get("SUPABASE_BUCKET_NAME");
        System.out.println(Url+" "+Key+" "+bucketName);
        //Báo lỗi ngay lập tức nếu thiếu cấu hình
        if (Url == null || Key == null || bucketName == null) {
            throw new RuntimeException("LỖI NGHIÊM TRỌNG: Không thể đọc được cấu hình Supabase từ file .env. Hãy kiểm tra lại đường dẫn file và tên biến!");
        }

        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).
                connectTimeout(Duration.ofSeconds(10)).build();
    }
    //Singleton
    public static StorageService getInstance() {
        if (instance == null) {
            synchronized (StorageService.class) {
                if (instance == null) {
                    instance = new StorageService();
                }
            }
        }
        return instance;
    }
    //Upload ảnh
    public String uploadImage(byte[] imageBytes, String extension) throws Exception {
        String uniqueFileName = UUID.randomUUID().toString() + (extension != null ? extension.toLowerCase() : ".png");
        String endpoint = String.format("%s/storage/v1/object/%s/%s", Url, bucketName, uniqueFileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " +Key)
                .header("Content-Type", determineContentType(extension))
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(imageBytes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return String.format("%s/storage/v1/object/public/%s/%s", Url, bucketName, uniqueFileName);
        } else {
            System.err.println("[Storage Error] Body: " + response.body());
            throw new RuntimeException("Tải ảnh thất bại. Status Code: " + response.statusCode());
        }
    }

    private String determineContentType(String extension) {
        if (extension == null) return "application/octet-stream";
        switch (extension.toLowerCase()) {
            case ".png":  return "image/png";
            case ".jpg":
            case ".jpeg": return "image/jpeg";
            case ".gif":  return "image/gif";
            case ".webp": return "image/webp";
            default:      return "application/octet-stream";
        }
    }
    // xóa ảnh cũ trên storage
    public void deleteImage(String publicUrl) {
        if (publicUrl == null || publicUrl.trim().isEmpty()) {
            return; // Không có ảnh cũ thì bỏ qua
        }

        try {
            // 1. Trích xuất tên file từ publicUrl
            // URL có dạng: https://.../storage/v1/object/public/auction-items/123-abc.jpg
            // Ta cần lấy đoạn "123-abc.jpg" ở cuối cùng
            String fileName = publicUrl.substring(publicUrl.lastIndexOf("/") + 1);

            // 2. Xây dựng Endpoint API để xóa (Lưu ý: Không có chữ 'public' trong endpoint xóa)
            // Format đúng: /storage/v1/object/[BUCKET_NAME]/[FILE_NAME]
            String endpoint = String.format("%s/storage/v1/object/%s/%s", Url, bucketName, fileName);

            // 3. Xây dựng request DELETE
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + Key)
                    .DELETE() // Dùng method DELETE
                    .build();

            // 4. Gửi yêu cầu
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                System.out.println("[Storage Info] Đã dọn dẹp ảnh cũ thành công: " + fileName);
            } else {
                System.err.println("[Storage Warning] Không thể xóa ảnh cũ. Status: " + response.statusCode() + ", Body: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("[Storage Error] Lỗi khi cố gắng xóa ảnh cũ: " + publicUrl);
            e.printStackTrace();
            // LƯU Ý: Ta chỉ in log chứ không throw Exception làm sập luồng Update Item.
            // Vì việc xóa file rác thất bại không nên cản trở việc User cập nhật thông tin thành công.
        }
    }
}
