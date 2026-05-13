package com.uet.BiddingApplication.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppExecutor {
    private static final Logger log = LoggerFactory.getLogger(AppExecutor.class);

    // Tạo Thread Pool với số lượng luồng bằng số nhân CPU (tối ưu nhất)
    private static final ExecutorService backgroundPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );
    /**
     * Chạy một tác vụ nặng ở luồng phụ (không gây đơ UI, không nghẽn mạng)
     */
    public static void execute(Runnable task) {
        backgroundPool.execute(task);
    }

    /**
     * Đóng Thread Pool khi thoát ứng dụng (Cực kỳ quan trọng)
     */
    public static void shutdown() {
        log.info("[AppExecutor] Đang đóng hệ thống luồng phụ...");
        backgroundPool.shutdown();
        try {
            if (!backgroundPool.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundPool.shutdownNow();
        }
    }
}