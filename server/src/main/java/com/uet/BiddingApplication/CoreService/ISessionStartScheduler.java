package com.uet.BiddingApplication.CoreService;

import java.time.LocalDateTime;

public interface ISessionStartScheduler {
    void scheduleStart(String sessionId, LocalDateTime startTime);
    void loadAllPendingSessions();
}
