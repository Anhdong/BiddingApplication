package com.uet.BiddingApplication.CoreService;

import com.uet.BiddingApplication.Config.AppConfig;
import com.uet.BiddingApplication.DAO.Impl.AuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Impl.BidDAO;
import com.uet.BiddingApplication.DAO.Interface.IAuctionSessionDAO;
import com.uet.BiddingApplication.DAO.Interface.IBidDAO;
import com.uet.BiddingApplication.DTO.Packet.ResponsePacket;
import com.uet.BiddingApplication.DTO.Request.BidRequestDTO;
import com.uet.BiddingApplication.DTO.Response.BidHistoryDTO;
import com.uet.BiddingApplication.DTO.Response.RealtimeUpdateDTO;
import com.uet.BiddingApplication.DTO.Response.SessionResultDTO;
import com.uet.BiddingApplication.DTO.Response.SessionTargetDTO;
import com.uet.BiddingApplication.Enum.ActionType;
import com.uet.BiddingApplication.Enum.SessionStatus;
import com.uet.BiddingApplication.Exception.BusinessException;
import com.uet.BiddingApplication.Model.AuctionSession;
import com.uet.BiddingApplication.Service.AutoBidManager;
import com.uet.BiddingApplication.Service.RealtimeBroadcastService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class InMemoryBidServiceImpl implements BidProcessingService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InMemoryBidServiceImpl.class);

    private static  InMemoryBidServiceImpl instance;
    private static final int WORKER_POOL_SIZE = AppConfig.getWorkerPoolSize();

    private final List<BlockingQueue<BidTask>> workerQueues;
    private final ExecutorService workerPool;
    private final ScheduledExecutorService scheduler;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks;

    private record BidTask(BidRequestDTO request, String bidderId) {}

    private ISearchCacheManager searchCacheManager;
    private RealtimeBroadcastService realtimeBroadcastService;

    private IBidDAO  bidDAO;
    private IAuctionSessionDAO auctionSessionDAO;

    private InMemoryBidServiceImpl() {
        this.workerQueues = new ArrayList<>(WORKER_POOL_SIZE);
        this.workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.searchCacheManager = SearchCacheManager.getInstance();
        this.realtimeBroadcastService = RealtimeBroadcastService.getInstance();
        this.bidDAO = BidDAO.getInstance();
        this.auctionSessionDAO = AuctionSessionDAO.getInstance();

        for (int i = 0; i < WORKER_POOL_SIZE; i++) {
            BlockingQueue<BidTask> queue = new LinkedBlockingQueue<>();
            workerQueues.add(queue);

            final int workerId = i;
            workerPool.submit(() -> processLoop(workerId, queue));
        }

        int cores = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(cores);
    }

    public static InMemoryBidServiceImpl getInstance() {
        if(instance == null) {
            synchronized (InMemoryBidServiceImpl.class) {
                if(instance == null) {
                    instance = new InMemoryBidServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void enqueueBid(BidRequestDTO request, String bidderId) {
        String sessionId = request.getSessionId();
        int workerId = Math.abs(sessionId.hashCode()) % WORKER_POOL_SIZE;
        workerQueues.get(workerId).offer(new BidTask(request, bidderId));
    }

    @Override
    public void startSessionProcessor(String sessionId) {
        AuctionSession session = searchCacheManager.getSession(sessionId);
        if (session == null) return;

        // [SỬA LỖI Ở ĐÂY]: Đổi SECONDS thành MILLIS để có độ chính xác tuyệt đối
        long delayMillis = ChronoUnit.MILLIS.between(LocalDateTime.now(), session.getEndTime());

        ScheduledFuture<?> oldTask = scheduledTasks.get(sessionId);
        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        // Chỉ kết thúc ngay nếu độ trễ thực sự chạm đáy <= 0 mili-giây
        if (delayMillis <= 0) {
            handleAuctionEnd(sessionId);
            return;
        }

        // [SỬA LỖI Ở ĐÂY]: Lên lịch với TimeUnit.MILLISECONDS
        ScheduledFuture<?> futureTask = scheduler.schedule(() -> {
            try {
                log.info("[TIMER] Luồng đếm ngược kích hoạt đóng phiên: " + sessionId);
                handleAuctionEnd(sessionId);
            } catch (Throwable t) {
                log.error("[CRITICAL ERROR] Lỗi nghiêm trọng khi đang đóng phiên trong luồng ẩn: " + sessionId, t);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);

        scheduledTasks.put(sessionId, futureTask);
    }

    public void processLoop(int workerId, BlockingQueue<BidTask> queue) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                BidTask task = queue.take();
                try {
                    processSingleBid(task);
                }catch (BusinessException e) {
                    // [BỔ SUNG MỚI]: Bắt lỗi nghiệp vụ và gửi riêng cho user bị lỗi
                    ResponsePacket<Void> errorPacket = new ResponsePacket<>(
                            ActionType.PLACE_MANUAL_BID,
                            400,
                            e.getMessage(), // Hiển thị nguyên văn lỗi (Ví dụ: "Bạn đang là người dẫn đầu...")
                            null
                    );
                    realtimeBroadcastService.sendPrivateMessage(task.bidderId, errorPacket);
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker " + workerId + " bị ngắt.");
            } catch (Exception e) {
                log.error("Lỗi tại Worker " + workerId + ": " + e.getMessage());
                log.error("Đã xảy ra lỗi Exception:", e);
            }
        }
    }

    private void processSingleBid(BidTask task) {
        BidRequestDTO req = task.request();
        String sessionId = req.getSessionId();
        AuctionSession session = searchCacheManager.getSession(sessionId);

        // Phiên không tồn tại hoặc đã bị khóa (Ended)
        if (session == null || session.getStatus() != SessionStatus.RUNNING) return;
        // Chặn Seller tự đặt giá cho hàng của mình (Đã có)
        if (task.bidderId.equals(session.getSellerId())) {
            throw new BusinessException("Không thể tự đặt giá cho sản phẩm của chính mình.");
        }

        // [BỔ SUNG MỚI]: Chặn tự đôn giá (Self-bidding)
        if (task.bidderId.equals(session.getWinnerName())) {
            throw new BusinessException("Bạn đang là người dẫn đầu, không thể tự nâng giá.");
        }

        if (validateBid(req.getBidAmount(), session)) {
            // 1. Cập nhật Cache trên RAM
            BigDecimal oldPrice = session.getCurrentPrice();
            String oldWinner = session.getWinnerName();
            searchCacheManager.updatePriceInCache(sessionId, req.getBidAmount(), req.getBidderName());

            // 2. Chống Sniping
            boolean isUpdateTime=checkAndHandleAntiSniping(session);

            // Gửi xuống DB
            boolean dbSuccess = bidDAO.placeBidAtomicTransaction(
                    sessionId,
                    task.bidderId(),
                    req.getBidderName(),
                    req.getBidAmount(),
                    req.getBidType(),
                    session.getEndTime()
            );
            // Gửi xuống mạng
            if(dbSuccess) {
                // 3. Đóng gói DTO Phát thanh Realtime
                BidHistoryDTO newHistory = new BidHistoryDTO();
                newHistory.setBidAmount(req.getBidAmount());
                newHistory.setBidderName(req.getBidderName());
                newHistory.setTime(LocalDateTime.now());
                newHistory.setSessionId(sessionId);

                RealtimeUpdateDTO updateData = new RealtimeUpdateDTO();
                updateData.setLastBid(newHistory);
                long newEndTime=session.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                updateData.setRemainingMillis(newEndTime-System.currentTimeMillis());

                ResponsePacket<RealtimeUpdateDTO> packet = new ResponsePacket<>(
                        ActionType.REALTIME_PRICE_UPDATE, 200, "Có giá mới", updateData
                );
                realtimeBroadcastService.broadcast(sessionId, packet);
            }
            else{
                log.error("Database từ chối nhận giá " + req.getBidAmount() + " của user " + task.bidderId());

                // Khôi phục RAM về giá trị cũ để đồng bộ với Database
                searchCacheManager.updatePriceInCache(sessionId, oldPrice, oldWinner);
            }

            // 4. Kích hoạt AutoBid đệ quy (Nếu AutoBidManager đẩy giá, nó sẽ gọi lại enqueueBid)
            AutoBidManager.getInstance().triggerAutoBid(sessionId, req.getBidAmount(),task.bidderId());
        }
        else{
            throw new BusinessException("Giá không hợp lệ");
        }
    }

    public boolean validateBid(BigDecimal bidAmount, AuctionSession session) {
        if (LocalDateTime.now().isAfter(session.getEndTime())) {
            return false;
        }
        BigDecimal minimumRequired = session.getCurrentPrice()!=null?session.getCurrentPrice():session.getStartPrice();
        minimumRequired=minimumRequired.add(session.getBidStep());
        return bidAmount.compareTo(minimumRequired) >= 0;
    }

    public boolean checkAndHandleAntiSniping(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = ChronoUnit.MILLIS.between(now, session.getEndTime());

        if (secondsLeft < 30000) {
            // [VÁ LỖI]: Hủy Task đếm ngược cũ trước khi tạo Task mới
            ScheduledFuture<?> oldTask = scheduledTasks.get(session.getId());
            if (oldTask != null && !oldTask.isDone()) {
                oldTask.cancel(false); // false: Không ngắt nếu task đang thực thi dở dang
            }

            // Gia hạn thêm 30s
            session.setEndTime(session.getEndTime().plusSeconds(30));

            // Lên lịch đếm ngược mới
            startSessionProcessor(session.getId());
            return true;
        }
        return false;
    }

    public void handleAuctionEnd(String sessionId) {
        AuctionSession session = searchCacheManager.getSession(sessionId);
        if (session == null) return;

        // Double-check: Đảm bảo thực sự đã hết giờ (Đề phòng sai số của Scheduler)
        if (LocalDateTime.now().isBefore(session.getEndTime())) {
            startSessionProcessor(sessionId); // Lên lịch lại phần thời gian còn sót
            return;
        }

        // 1. Chuyển trạng thái
        session.setStatus(SessionStatus.FINISHED);
        boolean statusUpdated=AuctionSessionDAO.getInstance().updateStatus(sessionId, SessionStatus.FINISHED);

        // 2. Lưu đồng bộ xuống Database để chốt sổ (Giao tiếp với DAO)
        boolean dbUpdated = auctionSessionDAO.updatePriceAndWinner(
                sessionId, session.getCurrentPrice(),session.getWinnerName());

        if (dbUpdated&&statusUpdated) {
            String winner = (session.getWinnerName() != null)
                    ? session.getWinnerName()
                    : "NO_WINNER";
            SessionResultDTO result = new SessionResultDTO(
                    sessionId,
                    session.getCurrentPrice(),
                    session.getStatus(),
                    winner
            );
            // 3. Xóa dữ liệu phiên khỏi RAM để giải phóng bộ nhớ
            searchCacheManager.removeSession(sessionId);
            if(session.getWinnerName()!=null) searchCacheManager.removeItem(session.getItemId());
            scheduledTasks.remove(sessionId);

            // 4. Phát thanh tín hiệu KẾT THÚC cho phòng đấu giá
            ResponsePacket<SessionResultDTO> endPacket = new ResponsePacket<>(
                    ActionType.REALTIME_SESSION_END, 200, "Phiên đấu giá đã kết thúc!",result
            );
            realtimeBroadcastService.broadcast(sessionId, endPacket);

            // 5. Giải tán phòng (Unsubscribe tất cả)
            realtimeBroadcastService.closeRoom(sessionId);
            //6. Dọn autoBid
            AutoBidManager.getInstance().clearSessionQueue(sessionId);
            log.info("Đã đóng phiên "+sessionId);
        } else {
            // Nếu Database lỗi (deadlock, rớt mạng chớp nhoáng), ta không để phiên bị "treo" chết trên RAM.
            // Giải pháp: Đẩy ngược nhiệm vụ này vào Scheduler để hệ thống tự động thử lại sau 5 giây.

            log.error("CRITICAL: Cập nhật DB thất bại cho phiên " + sessionId + ". Hệ thống sẽ thử lại sau 5 giây...");

            ScheduledFuture<?> retryTask = scheduler.schedule(() -> {
                handleAuctionEnd(sessionId);
            }, 5, TimeUnit.SECONDS);

            // Ghi đè lại tay cầm (Future) mới vào Map để quản lý
            scheduledTasks.put(sessionId, retryTask);
        }
    }
    /**
     * Dành cho Admin/Hệ thống gọi khi phát hiện vi phạm và cần hủy ngay lập tức phiên đang chạy.
     */
    @Override
    public void forceCancelSession(String sessionId, String reason) {
        AuctionSession session = searchCacheManager.getSession(sessionId);
        if (session == null) return;

        // 1. Dừng ngay lập tức đồng hồ đếm ngược
        ScheduledFuture<?> task = scheduledTasks.get(sessionId);
        if (task != null && !task.isDone()) {
            task.cancel(true); // true: Cố gắng ngắt (interrupt) nếu đang chạy dở
            scheduledTasks.remove(sessionId);
        }

        // 2. Cập nhật Database thành CANCELED
        boolean dbUpdated = AuctionSessionDAO.getInstance().updateStatus(sessionId, SessionStatus.CANCELED);

        if (dbUpdated) {
            // 3. Xóa khỏi RAM
            searchCacheManager.removeSession(sessionId);
            AutoBidManager.getInstance().clearSessionQueue(sessionId);

            // 4. Phát thanh giải tán phòng với lý do cụ thể
            SessionTargetDTO sessionTargetDTO = new SessionTargetDTO();
            sessionTargetDTO.setSessionId(sessionId);
            ResponsePacket<SessionTargetDTO> cancelPacket = new ResponsePacket<>(
                    ActionType.REALTIME_SESSION_CANCELED,
                    403,
                    "Phiên đấu giá đã bị Admin hủy: " + reason,
                    sessionTargetDTO
            );

            realtimeBroadcastService.broadcast(sessionId, cancelPacket);
            realtimeBroadcastService.closeRoom(sessionId);

        } else {
            log.error("CRITICAL: Không thể Force Cancel phiên " + sessionId + " xuống Database.");
            // Có thể thêm logic Retry ở đây nếu cần thiết
        }
    }
}