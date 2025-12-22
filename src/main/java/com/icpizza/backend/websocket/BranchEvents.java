package com.icpizza.backend.websocket;
import com.icpizza.backend.dto.BaseAdminResponse;
import com.icpizza.backend.dto.UpdateWorkLoadLevelTO;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.enums.WorkLoadLevel;
import com.icpizza.backend.websocket.mapper.WebsocketOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class BranchEvents {
    private final SimpMessagingTemplate ws;
    private final WebsocketOrderMapper pushMapper;
    private final Map<Long, Integer> attempts = new ConcurrentHashMap<>();
    private final @Qualifier("orderAckScheduler") ThreadPoolTaskScheduler scheduler;

    private final Map<BaseAdminResponse, ScheduledFuture<?>> pendingWorkloadUpdate = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 2;

    public void onAdminBaseInfoChange(BaseAdminResponse baseAdminResponse) {
        String dest = "/topic/admin-base-info";
        ws.convertAndSend(dest, baseAdminResponse);

        attempts.putIfAbsent(Long.valueOf(baseAdminResponse.branchNumber()), 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(Long.valueOf(baseAdminResponse.branchNumber()), 0);
            if (prev >= MAX_ATTEMPTS) {
                pendingWorkloadUpdate.remove(Long.valueOf(baseAdminResponse.branchNumber()));
                attempts.remove(Long.valueOf(baseAdminResponse.branchNumber()));
                return;
            }
            attempts.put(Long.valueOf(baseAdminResponse.branchNumber()), prev + 1);
            ws.convertAndSend(dest, baseAdminResponse);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pendingWorkloadUpdate.put(baseAdminResponse, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pendingWorkloadUpdate.put(baseAdminResponse, future);
    }

    private void noop() {}
}
