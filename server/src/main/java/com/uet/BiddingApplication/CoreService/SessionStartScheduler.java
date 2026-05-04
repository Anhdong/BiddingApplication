package com.uet.BiddingApplication.CoreService;



import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Response.SessionTargetDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Service.RealtimeBroadcastService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

public class SessionStartScheduler implements ISessionStartScheduler {

    private static final SessionStartScheduler INSTANCE = new SessionStartScheduler();

    // 1 luồng là quá đủ vì chỉ làm nhiệm vụ "Bóp cò" (Trigger)
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Quản lý các Task chờ mở phiên (Hữu ích khi Admin muốn Đổi lịch/Hủy phiên)
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingStarts = new ConcurrentHashMap<>();

    private SessionStartScheduler() {}

    public static SessionStartScheduler getInstance() {
        return INSTANCE;
    }

    /**
     * Lên lịch mở phiên.
     * Gọi khi: 1. Bật Server. 2. Admin/Seller vừa tạo hoặc sửa phiên đấu giá.
     */
    @Override
    public void scheduleStart(String sessionId, LocalDateTime startTime) {
        long delaySeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), startTime);

        // Nếu trước đó đã có lịch cho phiên này, hủy lịch cũ (Trường hợp Admin dời lịch)
        ScheduledFuture<?> oldTask = pendingStarts.get(sessionId);
        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        if (delaySeconds <= 0) {
            triggerSession(sessionId); // Tới giờ thì bắn luôn
        } else {
            ScheduledFuture<?> newTask = scheduler.schedule(() -> {
                triggerSession(sessionId);
            }, delaySeconds, TimeUnit.SECONDS);
            pendingStarts.put(sessionId, newTask);
        }
    }

    /**
     * Nơi thực thi khi "chuông báo thức" kêu
     */
    private void triggerSession(String sessionId) {
        // 1. Cập nhật trạng thái xuống DB (Đồng bộ)
        boolean dbUpdated = AuctionSessionDAO.getInstance().updateStatus(sessionId, SessionStatus.RUNNING);

        if (dbUpdated) {
            // 2. Cập nhật RAM: Lấy từ Cache ra và set lại Status (O(1), không gọi DB)
            AuctionSession session = SearchCacheManager.getInstance().getSession(sessionId);
            if (session != null) {
                session.setStatus(SessionStatus.RUNNING);
            }

            // 3. Bàn giao cho CORE: Mở đồng hồ đếm ngược kết thúc phiên
            InMemoryBidServiceImpl.getInstance().startSessionProcessor(sessionId);

            // 4. Bắn Realtime cho các Client đang ở ngoài sảnh hoặc đang chờ trong phòng biết
            long endTime=session.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            SessionTargetDTO sessionTargetDTO = new SessionTargetDTO();
            sessionTargetDTO.setSessionId(sessionId);
            sessionTargetDTO.setRemainingMillis(endTime-System.currentTimeMillis());
            ResponsePacket<SessionTargetDTO> packet = new ResponsePacket<>(
                    ActionType.REALTIME_SESSION_STARTED, 200, "Phiên đấu giá đã bắt đầu!",sessionTargetDTO
            );
            RealtimeBroadcastService.getInstance().broadcast(sessionId, packet);

            // Xóa task khỏi bộ nhớ quản lý
            pendingStarts.remove(sessionId);
        } else {
            // Cơ chế Retry nếu Database bị nghẽn (Tương tự handleAuctionEnd)
            System.err.println("Lỗi mở phiên " + sessionId + " xuống DB. Thử lại sau 5s...");
            ScheduledFuture<?> retryTask = scheduler.schedule(() -> triggerSession(sessionId), 5, TimeUnit.SECONDS);
            pendingStarts.put(sessionId, retryTask);
        }
    }

    /**
     * Bootstrapper: Dành cho hàm main() khi Server vừa bật lên
     */
    @Override
    public void loadAllPendingSessions() {
        // Lấy toàn bộ các phiên có trạng thái OPEN (Chưa tới giờ)
        List<AuctionSession> openSessions = SearchCacheManager.getInstance().getSessionsByStatus(SessionStatus.OPEN);
        if (openSessions != null) {
            for (AuctionSession session : openSessions) {
                scheduleStart(session.getId(), session.getStartTime());
            }
        }
    }
}