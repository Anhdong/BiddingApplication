package com.uet.BiddingApplication.Util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

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

    /*
    * Dùng khi bạn đang ở luồng phụ và cần kết quả ngay để chạy dòng code tiếp theo.
    */
    public static <T> T getResultFromUI(Supplier<T> supplier) {
        // Nếu đang ở luồng UI rồi thì chạy luôn cho xong
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }

        // Nếu đang ở luồng phụ, tạo một FutureTask để "hẹn" kết quả
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Platform.runLater(task);

        try {
            // Dòng này sẽ bắt luồng phụ DỪNG LẠI và CHỜ luồng UI trả kết quả
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("[AppExecutor]Cannot get result from UI{}", e.getMessage());
            return null;
        }
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