package com.uet.BiddingApplication.Session;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Tiện ích lấy cấu hình Server (host + port) từ GitHub Gist.
 *
 * Cách dùng:
 *   RemoteConfigFetcher.ServerAddress addr = RemoteConfigFetcher.fetch();
 *   Socket socket = new Socket(addr.host, addr.port);
 *
 * Định dạng nội dung Gist (một dòng duy nhất):
 *   0.tcp.ap.ngrok.io:12345
 */
public class Remoteconfigfetcher {

    // -----------------------------------------------------------------------
    // ĐỔI URL NÀY thành raw URL của GitHub Gist của bạn.
    // Ví dụ: https://gist.githubusercontent.com/<user>/<gist_id>/raw/server.txt
    // -----------------------------------------------------------------------
    private static final String GIST_RAW_URL =
            "https://gist.githubusercontent.com/YOUR_USERNAME/YOUR_GIST_ID/raw/server.txt";

    private static final int TIMEOUT_SECONDS = 10;

    /** Kết quả trả về: host và port đã được parse sẵn. */
    public static class ServerAddress {
        public final String host;
        public final int    port;

        public ServerAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }

    /**
     * Gọi HTTP GET đến Gist, đọc nội dung dạng "host:port", trả về {@link ServerAddress}.
     *
     * @return ServerAddress chứa host và port của Server qua Ngrok
     * @throws Exception nếu mạng lỗi, định dạng sai, hoặc Gist không tồn tại
     */
    public static ServerAddress fetch() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GIST_RAW_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Không thể tải cấu hình từ Gist. HTTP status: " + response.statusCode()
            );
        }

        return parse(response.body().trim());
    }

    /**
     * Parse chuỗi "host:port" thành {@link ServerAddress}.
     * Package-private để có thể unit test dễ dàng.
     */
    static ServerAddress parse(String raw) {
        // Hỗ trợ IPv6 dạng [::1]:8080 — tách từ dấu ':' cuối cùng
        int lastColon = raw.lastIndexOf(':');
        if (lastColon < 0) {
            throw new IllegalArgumentException(
                    "Định dạng Gist sai. Cần 'host:port', nhận được: '" + raw + "'"
            );
        }

        String host = raw.substring(0, lastColon).trim();
        String portStr = raw.substring(lastColon + 1).trim();

        if (host.isEmpty()) {
            throw new IllegalArgumentException("Host không được rỗng.");
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Port không hợp lệ: '" + portStr + "'"
            );
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "Port nằm ngoài phạm vi hợp lệ (1-65535): " + port
            );
        }

        return new ServerAddress(host, port);
    }
}