package com.icpizza.backend.service;

import com.icpizza.backend.dto.ShiftEventRequest;
import com.icpizza.backend.dto.ShiftEventResponse;
import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.repository.EventRepository;
import com.icpizza.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftService {
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");
    private final EventRepository eventRepo;
    private  final OrderRepository orderRepo;

    @Transactional
    public ShiftEventResponse createEvent(ShiftEventRequest request){
        if (request == null || request.branchId() == null || request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branch_id and type are required");
        }

        Integer lastShiftNo = eventRepo.findLastShiftNo(request.branchId());
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
                Event open = eventRepo
                        .findTopByBranchIdAndTypeAndShiftNoOrderByDatetimeDesc(request.branchId(),
                                EventType.OPEN_SHIFT_CASH_CHECK, shiftNo)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "open shift not found"));

                LocalDateTime from = open.getDatetime();
                LocalDateTime to = LocalDateTime.now(BAHRAIN);

                BigDecimal initialCash = open.getCashAmount() == null
                        ? BigDecimal.ZERO
                        : open.getCashAmount();

                BigDecimal cashOrders = orderRepo.sumCashBetween(from, to);
                if (cashOrders == null) cashOrders = BigDecimal.ZERO;

                BigDecimal expected = initialCash.add(cashOrders).setScale(2, RoundingMode.HALF_UP);
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

        eventRepo.save(ev);

        return new ShiftEventResponse("created", ev.getId(), shiftNo, warning);
    }

    public Map<String, EventType> getLastStage(String branchId) {
        Optional<Event> event = eventRepo.findFirstByBranchIdOrderByDatetimeDescIdDesc(branchId);
        Map<String, EventType> payload = new HashMap<>();

        if (event.isPresent()){
            payload.put("type", event.get().getType());
        }
        else payload.put("type", null);

        return payload;
    }
}
