package com.icpizza.backend.service;

import com.icpizza.backend.dto.*;
import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.enums.WorkLoadLevel;
import com.icpizza.backend.repository.*;
import com.icpizza.backend.websocket.BranchEvents;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final EventRepository eventRepository;
    private final TransactionRepository transactionRepository;

    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

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

            branchEvents.onAdminBaseInfoChange(getAdminBaseInfo(branch.getBranchNumber()));
        }

    }

    private WorkLoadLevel calculateWorkloadLevel(int activeItems) {
        if (activeItems < 6) return WorkLoadLevel.IDLE;
        if (activeItems < 11) return WorkLoadLevel.BUSY;
        if (activeItems < 16) return WorkLoadLevel.CROWDED;
        return WorkLoadLevel.OVERLOADED;
    }

    @Transactional
    public boolean setWorkloadLevel(UpdateWorkLoadLevelTO updateWorkLoadLevelTO) {
        log.info("[BRANCH WORKLOAD] setWorkloadLevel "+updateWorkLoadLevelTO.level()+"");
        Branch branch = branchRepository.findByBranchNumber(updateWorkLoadLevelTO.branchNumber());

        if(branch == null) return false;

        branch.setWorkLoadLevel(updateWorkLoadLevelTO.level());
        branchRepository.save(branch);
        BaseAdminResponse baseAdminResponse = getAdminBaseInfo(updateWorkLoadLevelTO.branchNumber());
        branchEvents.onAdminBaseInfoChange(baseAdminResponse);
        return true;
    }

    public int getEstimationByBranch(Branch branch) {
        return estimation.get(branch.getWorkLoadLevel()) + BASE_ORDER_TIME;
    }

    public WorkLoadLevel getWorkLoadLevel(Integer branchNumber) {
        Branch branch = branchRepository.findByBranchNumber(branchNumber);
        return branch.getWorkLoadLevel();
    }

    public BranchTO getBranchInfo(Integer branchNumber) {
        Branch branch = branchRepository.findByBranchNumber(branchNumber);
        return new BranchTO(branch.getId(), branch.getExternalId(), branchNumber, branch.getBranchName());
    }

    public BaseAdminResponse getAdminBaseInfo(Integer branchNumber) {
        log.info("[ADMIN BASE INFO] getAdminBaseInfo "+branchNumber);
        WorkLoadLevel level = getWorkLoadLevel(branchNumber);
        EventType cashType = getLastCashStage(branchNumber.toString());
        EventType shiftType = getLastShiftStage(branchNumber.toString());
        log.info("[BRANCH ADMIN BASE INFO]" + level + cashType + shiftType);

        return new BaseAdminResponse(level, cashType, shiftType, branchNumber);
    }

    @Transactional
    public ShiftEventResponse createEvent(ShiftEventRequest request){
        if (request == null || request.branchId() == null || request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branch_id and type are required");
        }

        Integer lastShiftNo = eventRepository.findLastShiftNo(request.branchId());
        int shiftNo;

        if(request.type() == EventType.OPEN_SHIFT_CASH_CHECK){
            shiftNo = (lastShiftNo == null ? 0 : lastShiftNo) + 1;
        }
        else{
            if (lastShiftNo == null || lastShiftNo == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no shift started yet");
            }
            shiftNo = lastShiftNo;
        }

        ShiftEventResponse.CashWarning warning = null;
        if (request.type() == EventType.CLOSE_SHIFT_CASH_CHECK) {
            if (request.cashAmount() != null) {
                Event open = eventRepository
                        .findTopByBranchIdAndTypeAndShiftNoOrderByDatetimeDesc(request.branchId(),
                                EventType.OPEN_SHIFT_CASH_CHECK, shiftNo)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "open shift not found"));

                LocalDateTime from = open.getDatetime();
                LocalDateTime to = LocalDateTime.now(BAHRAIN);

                BigDecimal initialCash = open.getCashAmount() == null
                        ? BigDecimal.ZERO
                        : open.getCashAmount();

                Branch branch = branchRepository.findByBranchNumber(1);

                BigDecimal cashTransactions = transactionRepository.sumCashBetween(from, to, branch.getId());
                if (cashTransactions == null) cashTransactions = BigDecimal.ZERO;

                BigDecimal expected = initialCash.add(cashTransactions).setScale(2, RoundingMode.HALF_UP);
                BigDecimal entered = request.cashAmount().setScale(2, RoundingMode.HALF_UP);

                if (entered.compareTo(expected) != 0) {
                    warning = new ShiftEventResponse.CashWarning("Amounts doesn't match", expected);
                }
            }
        }

        Event ev = new Event();
        ev.setId(java.util.UUID.randomUUID().toString());
        ev.setType(request.type());
        ev.setDatetime(LocalDateTime.now(BAHRAIN));
        ev.setPrepPlan(request.prepPlan());
        ev.setCashAmount(request.cashAmount());
        ev.setBranchId(request.branchId());
        ev.setShiftNo(shiftNo);

        eventRepository.save(ev);

        BaseAdminResponse baseAdminResponse = getAdminBaseInfo(Integer.valueOf(request.branchId()));

        branchEvents.onAdminBaseInfoChange(baseAdminResponse);

        return new ShiftEventResponse("created", ev.getId(), shiftNo, warning);
    }

    public EventType getLastCashStage(String branchId) {
        Event event = eventRepository.findFirstByBranchIdAndCashAmountIsNotNullOrderByDatetimeDescIdDesc(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return event.getType();
    }

    public EventType getLastShiftStage(String branchId) {
        Event event = eventRepository.findFirstByBranchIdAndCashAmountIsNullOrderByDatetimeDescIdDesc(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return event.getType();
    }
}
