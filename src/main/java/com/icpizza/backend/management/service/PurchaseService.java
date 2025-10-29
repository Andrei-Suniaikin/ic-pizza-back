package com.icpizza.backend.management.service;

import com.icpizza.backend.management.dto.BasePurchaseResponse;
import com.icpizza.backend.management.dto.CreatePurchaseTO;
import com.icpizza.backend.management.dto.EditPurchaseTO;
import com.icpizza.backend.management.dto.PurchaseTO;
import com.icpizza.backend.management.entity.PurchaseProduct;
import com.icpizza.backend.management.entity.PurchaseReport;
import com.icpizza.backend.management.mapper.PurchaseMapper;
import com.icpizza.backend.management.repository.PurchaseProductRepository;
import com.icpizza.backend.management.repository.PurchaseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;
    private final PurchaseProductRepository purchaseProductRepository;
    private final ProductService productService;

    public List<BasePurchaseResponse> getPurchaseReports(){
        List<PurchaseReport> reports = purchaseRepository.findAll();
        return purchaseMapper.toBasePurchaseResponse(reports);
    }

    @Transactional
    public BasePurchaseResponse createPurchaseReport(CreatePurchaseTO purchaseTO) {
        log.info("[CREATE PURCHASE] Creating purchase report for {}", purchaseTO);
        PurchaseReport purchaseReport = purchaseMapper.toPurchaseReportEntity(purchaseTO);
        purchaseRepository.save(purchaseReport);
        List<PurchaseProduct> purchaseProducts = purchaseMapper.toPurchaseProductsEntity(purchaseReport, purchaseTO.purchaseProducts());
        purchaseProductRepository.saveAll(purchaseProducts);
        if(!purchaseProducts.isEmpty()){
            int updatedProducts = productService.overwritePrices(purchaseProducts);
            log.info("[CREATE PURCHASE] Updated prices for "+updatedProducts+" products");
        }

        return purchaseMapper.toBasePurchaseResponse(purchaseReport);
    }

    public PurchaseTO getPurchaseReport(Long id) {
        PurchaseReport purchaseReport = purchaseRepository.findById(id).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND));

        List<PurchaseProduct> purchaseProducts = purchaseProductRepository.findAllByPurchaseReport(purchaseReport.getId());
        try {
            return (purchaseMapper.toPurchaseTO(purchaseReport, purchaseProducts));
        }
        catch (Exception e){
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public BasePurchaseResponse editPurchaseReport(EditPurchaseTO editpurchaseTO) {
        log.info("[EDIT PURCHASE REPORT] Editing purchase report with id: "+editpurchaseTO.id()+"");
        PurchaseReport purchaseReportToEdit = purchaseRepository.findById(editpurchaseTO.id()).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND));
        try {
            List<PurchaseProduct> oldPurchaseProducts = purchaseProductRepository.findAllByPurchaseReport(purchaseReportToEdit.getId());
            purchaseProductRepository.deleteAll(oldPurchaseProducts);
            purchaseReportToEdit.setFinalPrice(editpurchaseTO.finalPrice());
            purchaseRepository.save(purchaseReportToEdit);
            List<PurchaseProduct> newPurchaseProducts = purchaseMapper.toPurchaseProductsEntity(editpurchaseTO.purchaseProducts(), purchaseReportToEdit);
            purchaseProductRepository.saveAll(newPurchaseProducts);
            if (!newPurchaseProducts.isEmpty()) {
                int updatedProducts = productService.overwritePrices(newPurchaseProducts);
                log.info("[CREATE PURCHASE] Updated prices for " + updatedProducts + " products");
            }

            return purchaseMapper.toBasePurchaseResponse(purchaseReportToEdit);
        }
        catch (Exception e){ throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR); }
    }
}
