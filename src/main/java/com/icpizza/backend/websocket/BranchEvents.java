package com.icpizza.backend.websocket;
import com.icpizza.backend.dto.UpdateWorkLoadLevelTO;
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

    private final Map<UpdateWorkLoadLevelTO, ScheduledFuture<?>> pendingWorkloadUpdate = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 2;

    public void pushWorkloadLevelChange(UpdateWorkLoadLevelTO updateWorkLoadLevelTO){
        int branchNumber = updateWorkLoadLevelTO.branchNumber();
        String dest = "/topic/workload-level-change";
        ws.convertAndSend(dest, updateWorkLoadLevelTO);

        attempts.putIfAbsent(Long.valueOf(branchNumber), 0);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            int prev = attempts.getOrDefault(Long.valueOf(branchNumber), 0);
            if (prev >= MAX_ATTEMPTS) {
                pendingWorkloadUpdate.remove(Long.valueOf(branchNumber));
                attempts.remove(Long.valueOf(branchNumber));
                return;
            }
            attempts.put(Long.valueOf(branchNumber), prev + 1);
            ws.convertAndSend(dest, updateWorkLoadLevelTO);
            ScheduledFuture<?> again = scheduler.schedule(this::noop, new java.util.Date(System.currentTimeMillis() + 3000));
            pendingWorkloadUpdate.put(updateWorkLoadLevelTO, again);
        }, new java.util.Date(System.currentTimeMillis() + 3000));

        pendingWorkloadUpdate.put(updateWorkLoadLevelTO, future);
    }

    private void noop() {}
}
