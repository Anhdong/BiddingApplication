package com.uet.BiddingApplication.Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionPool {
    private static final HikariDataSource dataSource = initDataSource();
    private static HikariDataSource initDataSource() {
        //kiểm tra file .env nằm ở đâu
        String directory = new File(".env").exists() ? "./" : "../";

        //Đọc file .env
        Dotenv dotenv = Dotenv.configure().directory(directory).ignoreIfMissing().load();

        //Khởi tạo cấu hình cho HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dotenv.get("DB_URL"));
        config.setUsername(dotenv.get("DB_USER"));
        config.setPassword(dotenv.get("DB_PASSWORD"));

        // Tối ưu hóa Performance (Tuning) cho Hikari
        // Sử dụng giá trị default nếu trong .env không khai báo
        int poolSize = Integer.parseInt(dotenv.get("DB_POOL_SIZE", "10"));
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Integer.parseInt(dotenv.get("DB_MIN_IDLE", "2")));

        config.setConnectionTimeout(30000); // Timeout sau 30s nếu không lấy được connection
        config.setIdleTimeout(600000);      // 10 phút không dùng sẽ đóng connection
        config.setMaxLifetime(1800000);     // 30 phút là tuổi thọ tối đa của 1 connection (tránh leak)

        HikariDataSource ds = new HikariDataSource(config);
        //ĐĂNG KÝ SHUTDOWN HOOK NGAY TẠI ĐÂY
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (ds != null && !ds.isClosed()) {
                ds.close();
                System.out.println("Hệ thống tắt: Đã giải phóng Pool thành công.");
            }
        }));

        return ds;
    }
    public static HikariDataSource getDataSource() {
        return dataSource;
    }
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
