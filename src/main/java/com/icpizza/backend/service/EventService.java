package com.icpizza.backend.service;

import com.icpizza.backend.dto.branch.CashRegisterEventTO;
import com.icpizza.backend.entity.Event;
import com.icpizza.backend.enums.EventType;
import com.icpizza.backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public List<CashRegisterEventTO> getEvents(UUID branchId) {
        return eventRepository.getEvents(branchId, EventType.CASH_IN.toString(), EventType.CASH_OUT.toString())
                .stream()
                .map(this::toCashRegisterTO)
                .toList();
    }

    private CashRegisterEventTO toCashRegisterTO(Event event){
        return new CashRegisterEventTO(
                event.getId(),
                event.getNotes(),
                event.getBranch().getId(),
                event.getCashAmount(),
                event.getType(),
                event.getDatetime()
        );
    }
}
