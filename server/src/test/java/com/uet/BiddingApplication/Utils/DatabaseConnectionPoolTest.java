package com.uet.BiddingApplication.Utils;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionPoolTest {
    @Test
    void testGetConnection() {
        // Kiểm tra xem có lấy được kết nối không
        try (Connection conn = DatabaseConnectionPool.getConnection()) {
            assertNotNull(conn, "Kết nối không được null");
            assertTrue(conn.isValid(2), "Kết nối phải đang hoạt động");
        } catch (Exception e) {
            fail("Không thể kết nối đến Database: " + e.getMessage());
        }
    }
    @Test
    void verifyPoolSize() throws SQLException {
        // 1. Lấy đối tượng Pool
        HikariDataSource ds = DatabaseConnectionPool.getDataSource();

        // 2. Lấy đối tượng MXBean để xem thông số nội bộ
        HikariPoolMXBean poolProxy = ds.getHikariPoolMXBean();

        // In ra cấu hình tối đa bạn đã đặt trong .env
        int maxPoolSize = ds.getMaximumPoolSize();
        System.out.println("=== Cấu hình MaximumPoolSize: " + maxPoolSize);

        // 3. Mượn thử 3 kết nối
        Connection conn1 = ds.getConnection();
        Connection conn2 = ds.getConnection();
        Connection conn3 = ds.getConnection();

        // 4. Kiểm tra thông số thực tế
        System.out.println("--- Sau khi mượn 3 kết nối ---");
        System.out.println("Đang bận (Active): " + poolProxy.getActiveConnections());
        System.out.println("Đang rảnh (Idle): " + poolProxy.getIdleConnections());
        System.out.println("Tổng số kết nối hiện có (Total): " + poolProxy.getTotalConnections());

        // Xác minh bằng code: Active phải bằng 3
        assertEquals(3, poolProxy.getActiveConnections());

        // 5. Trả lại kết nối
        conn1.close();
        conn2.close();
        conn3.close();

        System.out.println("--- Sau khi trả lại toàn bộ ---");
        System.out.println("Đang bận (Active): " + poolProxy.getActiveConnections());
        System.out.println("Đang rảnh (Idle): " + poolProxy.getIdleConnections());

        // Xác minh bằng code: Active phải về 0
        assertEquals(0, poolProxy.getActiveConnections());
    }
}
