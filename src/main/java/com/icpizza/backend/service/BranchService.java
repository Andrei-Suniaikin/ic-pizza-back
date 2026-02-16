package com.icpizza.backend.service;

import com.icpizza.backend.dto.branch.*;
import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.CashUpdateType;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.enums.WorkLoadLevel;
import com.icpizza.backend.management.dto.VatResponse;
import com.icpizza.backend.repository.*;
import com.icpizza.backend.websocket.BranchEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private final CashRegisterService cashRegisterService;

    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    List<String> categories = List.of("Pizzas", "Combo Deals", "Brick Pizzas");

    Map<WorkLoadLevel, Integer> estimation = Map.of(WorkLoadLevel.IDLE, 0, WorkLoadLevel.BUSY, 10, WorkLoadLevel.CROWDED, 20, WorkLoadLevel.OVERLOADED, 30);
    int BASE_ORDER_TIME = 15;

    @Transactional
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
            log.info("[BRANCH WORKLOAD] new level set to "+newLevel+" ");

            branchEvents.onAdminBaseInfoChange(getAdminBaseInfo(branch.getId()));
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
        Optional<Branch> branchOpt = branchRepository.findById(updateWorkLoadLevelTO.branchId());

        if(branchOpt.isEmpty()) return false;
        Branch branch = branchOpt.get();
        branch.setWorkLoadLevel(updateWorkLoadLevelTO.level());
        branchRepository.save(branch);
        BaseAdminResponse baseAdminResponse = getAdminBaseInfo(updateWorkLoadLevelTO.branchId());
        branchEvents.onAdminBaseInfoChange(baseAdminResponse);
        return true;
    }

    @Transactional(readOnly = true)
    public int getEstimationByBranch(Branch branch) {
        return estimation.get(branch.getWorkLoadLevel()) + BASE_ORDER_TIME;
    }

    @Transactional(readOnly = true)
    public WorkLoadLevel getWorkLoadLevel(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        return branch.getWorkLoadLevel();
    }

    @Transactional(readOnly = true)
    public BranchTO getBranchInfo(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        return toBranchTO(branch);
    }

    @Transactional(readOnly = true)
    public BaseAdminResponse getAdminBaseInfo(UUID branchId) {
        log.info("[ADMIN BASE INFO] getAdminBaseInfo "+branchId);
        WorkLoadLevel level = getWorkLoadLevel(branchId);
        EventType cashType = getLastCashStage(branchId);
        EventType shiftType = getLastShiftStage(branchId);
        log.info("[ADMIN BASE INFO] base info: {}, {}, {}",  level, cashType, shiftType);

        return new BaseAdminResponse(level, cashType, shiftType, branchId);
    }

    @Transactional
    public ShiftEventResponse createEvent(ShiftEventRequest request){
        if (request == null || request.branchId() == null || request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branch_id and type are required");
        }

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        Integer lastShiftNo = eventRepository.findLastShiftNo(branch);
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
                BigDecimal expected = branch.getBranchBalance().setScale(2, RoundingMode.HALF_UP);
                BigDecimal entered = request.cashAmount().setScale(2, RoundingMode.HALF_UP);

                if (entered.compareTo(expected) != 0) {
                    warning = new ShiftEventResponse.CashWarning("Amounts doesn't match", expected);
                }
            }
        }
        else if (request.type() == EventType.OPEN_SHIFT_CASH_CHECK) {
            if (request.cashAmount() != null) {
                BigDecimal expected = branch.getBranchBalance().setScale(2, RoundingMode.HALF_UP);
                BigDecimal entered = request.cashAmount().setScale(2, RoundingMode.HALF_UP);

                if (entered.compareTo(expected) != 0) {
                    warning = new ShiftEventResponse.CashWarning("Amounts doesn't match", expected);
                }
            }
        }

        Event ev = new Event();
        ev.setType(request.type());
        ev.setDatetime(LocalDateTime.now(BAHRAIN));
        ev.setNotes(request.prepPlan());
        ev.setCashAmount(request.cashAmount());
        ev.setBranch(branch);
        ev.setShiftNo(shiftNo);

        eventRepository.save(ev);

        BaseAdminResponse baseAdminResponse = getAdminBaseInfo(request.branchId());

        branchEvents.onAdminBaseInfoChange(baseAdminResponse);

        return new ShiftEventResponse("created", ev.getId(), shiftNo, warning);
    }

    @Transactional(readOnly = true)
    public EventType getLastCashStage(UUID branchId) {
        return eventRepository.findLastCashEvent(branchId)
                .map(Event::getType)
                .orElse(EventType.CLOSE_SHIFT_CASH_CHECK);
    }

    @Transactional(readOnly = true)
    public EventType getLastShiftStage(UUID branchId) {
        return eventRepository.findLastDefaultEvent(branchId)
                .map(Event::getType)
                .orElse(EventType.CLOSE_SHIFT_EVENT);
    }


    @Transactional(readOnly = true)
    public List<BranchTO> getAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        return branches.stream()
                .map(this::toBranchTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchBalanceResponse getBranchBalance(UUID branchId) {
        return new BranchBalanceResponse(branchRepository.getBranchBalanceById(branchId));
    }

    private BranchTO toBranchTO(Branch branch) {
        return new BranchTO(
                branch.getId(),
                branch.getExternalId(),
                branch.getBranchNumber(),
                branch.getBranchName(),
                branch.getBranchBalance()
        );
    }

    @Transactional(readOnly = true)
    public VatResponse getVatStats(UUID branchId, LocalDate fromDate, LocalDate toDate) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        return orderRepository.getVatStat(branch.getId(), fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public BranchBalanceResponse cashUpdate(CashUpdateRequest request) {
        return new BranchBalanceResponse(
                processCashUpdate(request.branchId(), request.amount(), request.cashUpdateType(), request.note())
        );
    }

    private BigDecimal processCashUpdate(UUID branchId, BigDecimal cashAmount, CashUpdateType type, String note) {
        log.info("[CASH REGISTER] Updating cash balance...");
        Branch branch = branchRepository.findByIdWithLock(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        BigDecimal branchBalance = branch.getBranchBalance() == null ? BigDecimal.ZERO : branch.getBranchBalance();
        BigDecimal result;

        if (type.equals(CashUpdateType.CASH_IN)){
            result = branchBalance.add(cashAmount);
            branch.setBranchBalance(result);
            branchRepository.save(branch);
        }
        else {
            if (branchBalance.subtract(cashAmount).compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch balance not enough");
            }
            else  {
                result = branchBalance.subtract(cashAmount);
                branch.setBranchBalance(result);
                branchRepository.save(branch);
            }
        }

        cashRegisterService.createCashRegisterTransaction(
                new CashUpdateRequest(branchId, type, cashAmount, note)
        );

        return result;
    }
}
