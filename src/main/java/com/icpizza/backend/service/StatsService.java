package com.icpizza.backend.service;

import com.icpizza.backend.dto.DoughUsageTO;
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
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    @Transactional(readOnly = true)
    public StatsResponse getStatistics(LocalDate startDate, LocalDate finishDate, LocalDate certainDate) {
        log.info("Getting Stats...");
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
                jahezRevenue,
                getDoughUsage()
        );
    }

    private List<DoughUsageTO> getDoughUsage() {
        var rawUsage = orderRepo.getRawDoughUsage();
        log.debug("[STATS] getDoughUsage: " + rawUsage.toString());

        class Counters {final int[] range = new int[7];}

        Map<String, Counters> acc = new LinkedHashMap<>();

        for (Object[] row : rawUsage) {
            String doughType = (String) row[0];

            LocalDate shiftDate;

            Object d = row[1];
            if (d instanceof java.sql.Date sqlDate) {
                shiftDate = sqlDate.toLocalDate();
            }
            else if (d instanceof java.sql.Timestamp timestamp) {
                shiftDate = timestamp.toLocalDateTime().toLocalDate();
            }
            else if (d instanceof LocalDate ld) {
                shiftDate = ld;
            }
            else {
                throw new IllegalStateException("Unexpected date type: " + d);
            }
            int qty = ((Number) row[2]).intValue();

            int idx = switch (shiftDate.getDayOfWeek()) {
                case FRIDAY    -> 0;
                case SATURDAY  -> 1;
                case SUNDAY    -> 2;
                case MONDAY    -> 3;
                case TUESDAY   -> 4;
                case WEDNESDAY -> 5;
                case THURSDAY  -> 6;
            };

            acc.computeIfAbsent(doughType, k -> new Counters()).range[idx] += qty;
        }

        List<DoughUsageTO> doughUsage = new ArrayList<>(acc.size());
        for (var e : acc.entrySet()) {
            int[] c = e.getValue().range;
            doughUsage.add(new DoughUsageTO(
                    e.getKey(),
                    c[0], c[1], c[2], c[3], c[4], c[5], c[6]
            ));
        }

        log.info("[DOUGH STATS RAW] " + rawUsage.toString());
        log.info("[DOUGH STATS DTO] " + doughUsage.toString());

        return doughUsage;
    }

    private static BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private static Long nvl(Long x) { return x == null ? 0L : x; }
}
