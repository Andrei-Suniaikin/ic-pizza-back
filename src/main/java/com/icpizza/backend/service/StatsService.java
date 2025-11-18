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

        Map<String, Map<LocalDate, Integer>> dataMap = new LinkedHashMap<>();

        LocalDate today = LocalDate.now().minusDays(1);
        LocalDate minDate = today.minusDays(9);

        List<DoughUsageTO> doughUsage = new ArrayList<>();

        for(Object[] row : rawUsage) {
            String doughType = (String)row[0];
            LocalDate date = convertToLocalDate(row[1]);

            int quantity = ((Number) row[2]).intValue();

            dataMap.computeIfAbsent(doughType, k -> new LinkedHashMap<>())
                    .put(date, quantity);
        }

        for(String doughType : dataMap.keySet()) {
            List<DoughUsageTO.DoughDailyUsageTO> dailyUsage = new ArrayList<>();
            Map<LocalDate, Integer> amountOfDoughPerEachDay = dataMap.get(doughType);

            minDate.datesUntil(today.plusDays(1)).forEach(date -> {
                int quantity = amountOfDoughPerEachDay.get(date)!=null ? amountOfDoughPerEachDay.get(date) : 0;
                dailyUsage.add(new DoughUsageTO.DoughDailyUsageTO(date, quantity));
            });

            doughUsage.add(new DoughUsageTO(doughType, dailyUsage));
        }
        log.info("[STATS] getDoughUsage: " + doughUsage.toString());

        return doughUsage;
    }

    private LocalDate convertToLocalDate(Object d) {
        if (d == null) return LocalDate.now();
        if (d instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        } else if (d instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        } else if (d instanceof LocalDate ld) {
            return ld;
        } else {
            throw new IllegalStateException("Unexpected date type: " + d.getClass());
        }
    }

    private static BigDecimal nvl(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }
    private static Long nvl(Long x) { return x == null ? 0L : x; }
}
