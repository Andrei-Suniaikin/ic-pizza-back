package com.icpizza.backend.service;

import com.icpizza.backend.dto.StatsResponse;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.repository.CustomerRepository;
import com.icpizza.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    @Transactional(readOnly = true)
    public StatsResponse getStatistics(LocalDate startDate, LocalDate finishDate, LocalDate certainDate) {
        LocalDateTime start = startDate.atStartOfDay(BAHRAIN).toLocalDateTime().plusHours(2);
        LocalDateTime end   = finishDate.plusDays(1).atStartOfDay(BAHRAIN).toLocalDateTime()
                .plusHours(1).plusMinutes(59).plusSeconds(59);

        BigDecimal totalPickUpRevenue = nvl(orderRepo.sumAmountPaidBetweenAndOrderType(start, end, "Pick Up"));
        Long pickUpOrders = orderRepo.countByCreatedAtBetweenAndOrderType(start, end, "Pick Up");
        long uniqInWindow = orderRepo.countUniqueCustomersInWindow(start, end);
        long newUniq      = orderRepo.countNewUniqueCustomersInWindow(start, end);
        long oldUniq      = Math.max(0, uniqInWindow - newUniq);

        long jahezOrders   = orderRepo.countByCreatedAtBetweenAndOrderType(start, end, "Jahez");
        BigDecimal jahezRevenue = nvl(orderRepo.sumAmountPaidBetweenAndOrderType(start, end, "Jahez"));

        BigDecimal sumPaid = nvl(orderRepo.sumAllAmountPaidAllTime());
        long uniqueCustomers = customerRepo.countDistinctTelephoneNo();
        long sumOrders = customerRepo.sumAllAmountOfOrders();

        BigDecimal arpu = (uniqueCustomers == 0)
                ? BigDecimal.ZERO
                : sumPaid.divide(BigDecimal.valueOf(uniqueCustomers), 2, RoundingMode.HALF_UP);

        BigDecimal aov = (sumOrders == 0)
                ? BigDecimal.ZERO
                : sumPaid.divide(BigDecimal.valueOf(sumOrders), 2, RoundingMode.HALF_UP);

        long repeatCustomers = customerRepo.countRepeatCustomers();

        Long monthTotal = null, retained = null;
        BigDecimal retentionPct = null;

        if (certainDate != null) {
            LocalDate prevMonthStart = certainDate.withDayOfMonth(1).minusMonths(1);
            LocalDate currMonthStart = certainDate.withDayOfMonth(1);
            LocalDateTime currEndLdt = certainDate.atTime(23, 59, 59);

            Timestamp prevStartTs = Timestamp.valueOf(prevMonthStart.atStartOfDay());
            Timestamp currStartTs = Timestamp.valueOf(currMonthStart.atStartOfDay());
            Timestamp currEndTs   = Timestamp.valueOf(currEndLdt);

            monthTotal = nvl(orderRepo.countFirstTimeCustomersInPrevMonth(prevStartTs, currStartTs));
            retained   = nvl(orderRepo.countRetained(prevStartTs, currStartTs, currEndTs));
            retentionPct = (monthTotal > 0)
                    ? BigDecimal.valueOf(retained * 100.0 / monthTotal).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }


        log.info("[STATS] " + start + ", " + end + ", " +  pickUpOrders + ", " + newUniq + ", " + oldUniq);
        return new StatsResponse(
                totalPickUpRevenue.setScale(2, RoundingMode.HALF_UP),
                pickUpOrders,
                newUniq,
                oldUniq,
                arpu,
                uniqueCustomers,
                repeatCustomers,
                aov,
                monthTotal,
                retained,
                retentionPct,
                jahezOrders,
                jahezRevenue
        );
    }

    private static BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private static Long nvl(Long x) { return x == null ? 0L : x; }
}
