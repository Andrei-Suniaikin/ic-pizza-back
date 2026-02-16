package com.icpizza.backend.websocket;
import com.icpizza.backend.dto.branch.BaseAdminResponse;
import com.icpizza.backend.websocket.mapper.WebsocketOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class BranchEvents {
    private final SimpMessagingTemplate ws;
    private final WebsocketOrderMapper pushMapper;
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final @Qualifier("orderAckScheduler") ThreadPoolTaskScheduler scheduler;

    private final Map<BaseAdminResponse, ScheduledFuture<?>> pendingWorkloadUpdate = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 2;

    public void onAdminBaseInfoChange(BaseAdminResponse baseAdminResponse) {
        String dest = "/topic/"+ baseAdminResponse.branchId() + "/admin-base-info";
        ws.convertAndSend(dest, baseAdminResponse);

        attempts.putIfAbsent(baseAdminResponse.branchId(), 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(baseAdminResponse.branchId(), 0);
            if (prev >= MAX_ATTEMPTS) {
                pendingWorkloadUpdate.remove(baseAdminResponse.branchId());
                attempts.remove(baseAdminResponse.branchId());
                return;
            }
            attempts.put(baseAdminResponse.branchId(), prev + 1);
            ws.convertAndSend(dest, baseAdminResponse);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pendingWorkloadUpdate.put(baseAdminResponse, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pendingWorkloadUpdate.put(baseAdminResponse, future);
    }

    private void noop() {}
}
