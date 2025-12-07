package com.icpizza.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record StatsResponse(
        @JsonProperty("pick_up_total_revenue")
        BigDecimal totalPickUpRevenue,
        @JsonProperty("pick_up_total_order_count")
        Long totalPickUpOrderCount,
        @JsonProperty("new_customer_ordered_count")
        Long newCustomerOrderedCount,
        @JsonProperty("old_customer_ordered_count")
        Long oldCustomerOrderedCount,
        @JsonProperty("ARPU")
        BigDecimal arpu,
        @JsonProperty("unique_customers_all_time")
        Long uniqueCustomersAllTime,
        @JsonProperty("repeat_customers_all_time")
        Long repeatCustomersAllTime,
        @JsonProperty("average_order_value_all_time")
        BigDecimal averageOrderValueAllTime,
        @JsonProperty("month_total_customers")
        Long monthTotalCustomers,
        @JsonProperty("retained_customers")
        Long retainedCustomers,
        @JsonProperty("retention_percentage")
        BigDecimal retentionPercentage,
        @JsonProperty("jahez_total_order_count")
        Long totalJahezOrderCount,
        @JsonProperty("jahez_total_revenue")
        BigDecimal totalJahezRevenue,
        List<DoughUsageTO> doughUsageTOS,
        List<SellsByHourStat> sellsByHour,
        Long totalTalabatOrders,
        BigDecimal totalTalabatRevenue,
        List<TopFiveProducts> topProducts
) {
}
