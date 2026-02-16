package com.icpizza.backend.service;

import com.icpizza.backend.dto.branch.CashUpdateRequest;
import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.CashUpdateType;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CashRegisterService {
    private final EventRepository eventRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public void createCashRegisterTransaction(CashUpdateRequest request){
        Event cashRegisterTransaction = new Event();
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(()-> new RuntimeException("Branch not found"));
        cashRegisterTransaction.setNotes(request.note());
        cashRegisterTransaction.setBranch(branch);
        cashRegisterTransaction.setCashAmount(request.amount());
        cashRegisterTransaction.setType(convertToEventType(request.cashUpdateType()));
        cashRegisterTransaction.setDatetime(LocalDateTime.now(ZoneId.of("Asia/Bahrain")));
        eventRepository.save(cashRegisterTransaction);
    }

    private EventType convertToEventType(CashUpdateType cashUpdateType) {
        return switch (cashUpdateType) {
            case CASH_IN -> EventType.CASH_IN;
            case CASH_OUT -> EventType.CASH_OUT;
            default -> throw new IllegalArgumentException("Unknown type: " + cashUpdateType);
        };
    }
}
