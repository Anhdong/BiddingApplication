package com.uet.BiddingApplication.Utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

public class StorageServiceTest {

    private StorageService storageService;
    private HttpServer mockServer;
    private String mockServerUrl;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);

        mockServer.createContext("/storage/v1/object/mock-bucket", exchange -> {
            // Tiêu thụ (đọc) toàn bộ request body gửi lên trước khi phản hồi.
            // Điều này ngăn chặn lỗi TCP RST khiến HTTP/2 Client bị crash.
            try (java.io.InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                String response = "Upload thành công";
                // Chú ý: Dùng response.getBytes().length thay vì response.length() để tránh lỗi tiếng Việt
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        mockServer.start();
        mockServerUrl = "http://localhost:" + mockServer.getAddress().getPort();

        storageService = StorageService.getInstance();

        setPrivateField(storageService, "Url", mockServerUrl);
        setPrivateField(storageService, "Key", "fake-token-123");
        setPrivateField(storageService, "bucketName", "mock-bucket");
    }

    @AfterEach
    void tearDown() {
        // Tắt server ảo sau khi test xong để giải phóng cổng mạng
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    /**
     * Hàm Helper để tiêm Reflection.
     */
    private void setPrivateField(Object targetObject, String fieldName, Object value) throws Exception {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(targetObject, value);
    }

    // =========================================================================
    // TEST CASES
    // =========================================================================

    @Test
    @DisplayName("Upload ảnh thành công lên Server ảo")
    void testUploadImage_Success() throws Exception {
        // Arrange
        byte[] dummyImageBytes = new byte[]{ (byte) 0xFF, (byte) 0xD8 }; // Dữ liệu giả
        String extension = ".jpg";

        // Act
        // HttpClient sẽ thực sự gửi gói tin mạng HTTP, nhưng gửi tới http://localhost:...
        String resultUrl = storageService.uploadImage(dummyImageBytes, extension);

        // Assert
        assertNotNull(resultUrl);
        // Kiểm tra xem URL trả về có đúng format định sẵn không
        assertTrue(resultUrl.startsWith(mockServerUrl + "/storage/v1/object/public/mock-bucket/"));
        assertTrue(resultUrl.endsWith(".jpg"));
    }

    @Test
    @DisplayName("Upload ảnh thất bại - Server trả về lỗi 400")
    void testUploadImage_Failure() throws Exception {
        mockServer.removeContext("/storage/v1/object/mock-bucket");
        mockServer.createContext("/storage/v1/object/mock-bucket", exchange -> {

            // Đọc hết request body để không bị đứt luồng HTTP/2
            try (java.io.InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }

            String errorResponse = "Bad Request";
            exchange.sendResponseHeaders(400, errorResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        });

        byte[] dummyImageBytes = new byte[]{1, 2, 3};
        String extension = ".png";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            storageService.uploadImage(dummyImageBytes, extension);
        });

        assertTrue(exception.getMessage().contains("Status Code: 400"));
    }
}