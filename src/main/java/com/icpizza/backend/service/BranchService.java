package com.icpizza.backend.service;

import com.icpizza.backend.dto.UpdateWorkLoadLevelTO;
import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.enums.WorkLoadLevel;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.repository.OrderItemRepository;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.websocket.BranchEvents;
import com.icpizza.backend.websocket.OrderEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;
    private final BranchEvents branchEvents;

    List<String> categories = List.of("Pizzas", "Combo Deals", "Brick Pizzas");

    Map<WorkLoadLevel, Integer> estimation = Map.of(WorkLoadLevel.IDLE, 0, WorkLoadLevel.BUSY, 10, WorkLoadLevel.CROWDED, 20, WorkLoadLevel.OVERLOADED, 30);
    int BASE_ORDER_TIME = 15;

    public void recalcBranchWorkload(Branch branch) {
        log.info("Recalc Branch Workload...");
        List<Long> activeOrderIds = orderRepository.findActiveOrderIdsByBranch(branch.getBranchNumber());
        log.info("[RECALC WORKLOAD]" + activeOrderIds);
        if(activeOrderIds.isEmpty()){
            branch.setWorkLoadLevel(WorkLoadLevel.IDLE);
            branchRepository.save(branch);
            return;
        }

        int activeItems = activeOrderIds.stream()
                .flatMap(orderId -> orderItemRepository.findByOrderId(orderId).stream()
                        .filter(item -> categories.contains(item.getCategory())))
                    .mapToInt(item -> 1)
                    .sum();

        log.info("[RECALC WORKLOAD]" + activeItems);


        WorkLoadLevel newLevel = calculateWorkloadLevel(activeItems);

        if(newLevel.isHigherThan(branch.getWorkLoadLevel())){
            branch.setWorkLoadLevel(newLevel);
            branchRepository.save(branch);
            log.info("[BRANCH WORKLOAD] new level set to "+newLevel+"");
            UpdateWorkLoadLevelTO updateWorkLoadLevelTO = new UpdateWorkLoadLevelTO(newLevel, branch.getBranchNumber());
            branchEvents.pushWorkloadLevelChange(updateWorkLoadLevelTO);
        }

    }

    private WorkLoadLevel calculateWorkloadLevel(int activeItems) {
        if (activeItems < 6) return WorkLoadLevel.IDLE;
        if (activeItems < 11) return WorkLoadLevel.BUSY;
        if (activeItems < 16) return WorkLoadLevel.CROWDED;
        return WorkLoadLevel.OVERLOADED;
    }

    public boolean setWorkloadLevel(UpdateWorkLoadLevelTO updateWorkLoadLevelTO) {
        log.info("[BRANCH WORKLOAD] setWorkloadLevel "+updateWorkLoadLevelTO.level()+"");
        Branch branch = branchRepository.findByBranchNumber(updateWorkLoadLevelTO.branchNumber());

        if(branch == null) return false;

        branch.setWorkLoadLevel(updateWorkLoadLevelTO.level());
        branchRepository.save(branch);
        branchEvents.pushWorkloadLevelChange(updateWorkLoadLevelTO);
        return true;
    }

    public int getEstimationByBranch(Branch branch) {
        return estimation.get(branch.getWorkLoadLevel()) + BASE_ORDER_TIME;
    }

    public WorkLoadLevel getWorkLoadLevel(Integer branchNumber) {
        Branch branch = branchRepository.findByBranchNumber(branchNumber);
        return branch.getWorkLoadLevel();
    }
}
