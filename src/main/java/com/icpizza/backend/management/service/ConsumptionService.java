package com.icpizza.backend.management.service;

import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.User;
import com.icpizza.backend.management.dto.ConsumptionReportTO;
import com.icpizza.backend.management.dto.ProductInfo;
import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.entity.ProductConsumptionItem;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.enums.ReportType;
import com.icpizza.backend.management.mapper.ReportMapper;
import com.icpizza.backend.management.mapper.Titles;
import com.icpizza.backend.management.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumptionService {
    private final ReportRepository reportRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final PurchaseProductRepository purchaseProductRepository;
    private final EntityManager em;
    private final ProductRepository productRepository;
    private final ConsumptionItemRepository consumptionItemRepository;
    private final ConsumptionItemRepository consumptionItemItemRepository;

    private static final ZoneId TZ = ZoneId.of("Asia/Bahrain");
    private final ReportMapper reportMapper;

    @Transactional
    public void upsertByInventoryEvent(Branch branch, Long userId, YearMonth ym) {
        log.info("[CONSUMPTION REPORT] Started to create consumption report: " + ym);
        final String curPrefix = Titles.monthPrefix(ym);
        final String prevPrefix = Titles.monthPrefix(ym.minusMonths(1));

        final String consumptionTitle = curPrefix + "-consumption-" + branch.getBranchName().toLowerCase();


        Report report = reportRepository.findByBranchTypeTitle(branch.getId(), ReportType.PRODUCT_CONSUMPTION, consumptionTitle)
                .orElseGet(() -> {
                    Report r = new Report();
                    r.setBranch(branch);
                    r.setUser(em.getReference(User.class, userId));
                    r.setType(ReportType.PRODUCT_CONSUMPTION);
                    r.setTitle(consumptionTitle);
                    r.setCreatedAt(LocalDateTime.now(TZ));
                    r.setFinalPrice(BigDecimal.ZERO);
                    return reportRepository.save(r);
                });

        consumptionItemRepository.deleteByReportId(report.getId());

        Set<Long> purchIds = productRepository.findIdsByPurchasableTrue();
        Map<Long, ProductInfo> previousInventory = productsFromPreviousInventory(branch.getId(), prevPrefix, purchIds);
        log.info(previousInventory.toString());
        Map<Long, ProductInfo> currentInventory = productsFromCurrentInventory(branch.getId(), curPrefix, purchIds);
        Map<Long, ProductInfo> purchases = purchases(branch.getId(), curPrefix, purchIds);
        log.info(purchases.toString());

        Set<Long> keys = new HashSet<>();
        keys.addAll(previousInventory.keySet());
        keys.addAll(currentInventory.keySet());
        keys.addAll(purchases.keySet());

        Map<Long, Product> products = productRepository.findAllByIdIn(keys).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal total = BigDecimal.ZERO;
        List<ProductConsumptionItem> items = new ArrayList<>(keys.size());

        for (Long pid : keys) {
            var o = previousInventory.getOrDefault(pid, new ProductInfo(pid, BigDecimal.ZERO, BigDecimal.ZERO));
            var b = purchases.getOrDefault(pid, new ProductInfo(pid, BigDecimal.ZERO, BigDecimal.ZERO));
            var c = currentInventory.getOrDefault(pid, new ProductInfo(pid, BigDecimal.ZERO, BigDecimal.ZERO));

            BigDecimal totalQuantityForMonth = o.quantity().add(b.quantity()).subtract(c.quantity());
            BigDecimal totalFinalPrice = o.finalPrice().add(b.finalPrice()).subtract(c.finalPrice());

            if (totalQuantityForMonth.signum() < 0) totalQuantityForMonth = BigDecimal.ZERO;
            if (totalFinalPrice.signum() < 0) totalFinalPrice = BigDecimal.ZERO;

            Product p = products.get(pid);
            items.add(new ProductConsumptionItem(null, p, report, totalQuantityForMonth, totalFinalPrice));
            total = total.add(totalFinalPrice);
        }

        log.info("[CONSUMPTION REPORT] total sum: " + total);
        consumptionItemRepository.saveAll(items);
        report.setFinalPrice(total.setScale(3, RoundingMode.HALF_UP));
        reportRepository.save(report);
    }


    private Map<Long, ProductInfo> productsFromPreviousInventory(UUID branchId, String prevPrefix, Set<Long> ids) {
        Report prevInventory = pickLatest(branchId, ReportType.INVENTORY, prevPrefix);
        if (prevInventory == null) return Map.of();
        return inventoryProductRepository.loadByReport(prevInventory.getId(), ids).stream()
                .collect(Collectors.toMap(ProductInfo::id, x -> x));
    }

    private Map<Long, ProductInfo> productsFromCurrentInventory(UUID branchId, String curPrefix, Set<Long> ids) {
        Report curInventory = pickLatest(branchId, ReportType.INVENTORY, curPrefix);
        if (curInventory == null) return Map.of();
        return inventoryProductRepository.loadByReport(curInventory.getId(), ids).stream()
                .collect(Collectors.toMap(ProductInfo::id, x -> x));
    }

    private Map<Long, ProductInfo> purchases(UUID branchId, String curPrefix, Set<Long> ids) {
        var opt = reportRepository.findSinglePurchaseByPrefix(branchId, curPrefix);
        if (opt.isEmpty()) return Map.of();
        Long purchaseReportId = opt.get().getId();
        return purchaseProductRepository.aggregateForReport(purchaseReportId, ids).stream()
                .collect(Collectors.toMap(ProductInfo::id, x -> x));
    }

    private Report pickLatest(UUID branchId, ReportType type, String prefix) {
        List<Report> list = reportRepository.findByPrefix(branchId, type, prefix);
        return list.isEmpty() ? null : list.get(0);
    }

    public ConsumptionReportTO getLatestReport() {
        Report report = reportRepository.findTopByTypeOrderByCreatedAtDesc(ReportType.PRODUCT_CONSUMPTION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        List<ProductConsumptionItem> products = consumptionItemItemRepository.findAllByReport(report);

        return reportMapper.toConsumptionReportTO(report, products);
    }
}
