package com.icpizza.backend.management.service;

import com.icpizza.backend.management.dto.*;
import com.icpizza.backend.management.entity.InventoryProduct;
import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.mapper.ProductMapper;
import com.icpizza.backend.management.mapper.ReportMapper;
import com.icpizza.backend.management.repository.InventoryProductRepository;
import com.icpizza.backend.management.repository.ProductRepository;
import com.icpizza.backend.management.repository.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final ReportMapper reportMapper;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

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
    public void createReport(CreateReportTO createReportTO) {
        try {
            Report report = reportMapper.toReportEntity(createReportTO);
            reportRepository.saveAndFlush(report);
            List<InventoryProduct> reportProducts = reportMapper.toInventoryProducts(createReportTO.inventoryProducts(), report);
            log.info("[REPORT PRODUCTS] converting products from entity {}", reportProducts.toString());
            inventoryProductRepository.saveAll(reportProducts);
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void editReport(EditReportTO editReportTO) {
        try{
            Report report = reportRepository.findById(editReportTO.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            inventoryProductRepository.deleteAllByReport(report);

            List<InventoryProduct> newProducts = reportMapper.toInventoryProductsEntity(editReportTO.inventoryProducts(), report);
            inventoryProductRepository.saveAll(newProducts);
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    public List<ProductTO> fetchProducts() {
        try {
            List<Product> productEntities = productRepository.findAll();
            List<ProductTO> products = productMapper.toProductTO(productEntities);
            return products;
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
