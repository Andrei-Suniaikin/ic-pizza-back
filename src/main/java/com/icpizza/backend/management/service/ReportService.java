package com.icpizza.backend.management.service;

import com.icpizza.backend.management.dto.*;
import com.icpizza.backend.management.entity.InventoryProduct;
import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.enums.ReportType;
import com.icpizza.backend.management.mapper.ProductMapper;
import com.icpizza.backend.management.mapper.ReportMapper;
import com.icpizza.backend.management.mapper.Titles;
import com.icpizza.backend.management.repository.InventoryProductRepository;
import com.icpizza.backend.management.repository.ProductRepository;
import com.icpizza.backend.management.repository.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final ReportMapper reportMapper;
    private final ConsumptionService consumptionService;

    public List<BaseManagementResponse> getAllReportsByBranch(Integer branchNo) {
        try {
            List<Report> reports = reportRepository.findAllByBranchDesc(branchNo);
            return reportMapper.toBaseManagementResponse(reports);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public BaseManagementResponse createReport(CreateReportTO createReportTO) {
        try {
            Report report = reportMapper.toReportEntity(createReportTO);
            reportRepository.saveAndFlush(report);
            List<InventoryProduct> reportProducts = reportMapper.toInventoryProducts(createReportTO.inventoryProducts(), report);
            log.info("[REPORT PRODUCTS] converting products from entity {}", reportProducts.size());
            inventoryProductRepository.saveAll(reportProducts);

            YearMonth ym = Titles.parseYearMonthPrefix(report.getTitle());
            log.info("[REPORT] trying to create consumption product");
            consumptionService.upsertByInventoryEvent(report.getBranch(), report.getUser().getId(), ym);
            return reportMapper.toBaseManagementResponse(report);
        }
        catch(Exception e){
            log.error(e.getMessage(), " Failed to create consumption report");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating/updating consumption report");
        }
    }

    @Transactional
    public BaseManagementResponse editReport(EditReportTO editReportTO) {
        log.info("[INVENTORY REPORT] Editing inventory report with id: {}", editReportTO.id());
        try{
            Report report = reportRepository.findById(editReportTO.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            log.info("Updated final prtice from {} to {}", report.getFinalPrice(), editReportTO.finalPrice());
            report.setFinalPrice(editReportTO.finalPrice());
            reportRepository.saveAndFlush(report);

            inventoryProductRepository.deleteAllByReport(report);

            List<InventoryProduct> newProducts = reportMapper.toInventoryProductsEntity(editReportTO.inventoryProducts(), report);
            inventoryProductRepository.saveAll(newProducts);

            YearMonth ym = Titles.parseYearMonthPrefix(report.getTitle());
            log.info("[REPORT] trying to edit consumption report");
            consumptionService.upsertByInventoryEvent(report.getBranch(), report.getUser().getId(), ym);

            return reportMapper.toBaseManagementResponse(report);

        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Boolean checkIfExistsConsumptionForCurrentMonth(String title){
        log.info("Checking if exists consumption for current month: " + title);
        Report report = reportRepository.findTopByTypeOrderByCreatedAtDesc(ReportType.INVENTORY).orElseGet(()-> null);
        if (report == null){
            return false;
        }
        String inventoryTitlePrefix = report.getTitle().substring(0, 6);
        String purchasePrefix = title.substring(0, 6);
        return inventoryTitlePrefix.equals(purchasePrefix);
    }

    public ReportTO getReport(Long id) {
        try {
            Report report = reportRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            return reportMapper.toOrderTO(report);
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
