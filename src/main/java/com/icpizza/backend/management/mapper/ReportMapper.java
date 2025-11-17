package com.icpizza.backend.management.mapper;

import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.User;
import com.icpizza.backend.management.dto.*;
import com.icpizza.backend.management.entity.InventoryProduct;
import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.entity.ProductConsumptionItem;
import com.icpizza.backend.management.entity.Report;
import com.icpizza.backend.management.repository.InventoryProductRepository;
import com.icpizza.backend.management.repository.ProductRepository;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMapper {
    private final BranchRepository branchRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    public List<BaseManagementResponse> toBaseManagementResponse(List<Report> reports) {
        return reports.stream().map(report -> {
            return new BaseManagementResponse(
                    report.getId(),
                    report.getType(),
                    report.getTitle(),
                    report.getCreatedAt().toString(),
                    report.getBranch().getBranchNumber(),
                    report.getUser().getUserName(),
                    report.getFinalPrice()
            );
        }).toList();
    }

    public Report toReportEntity(CreateReportTO createReportTO) {
        Report report = new Report();
        Branch branch = branchRepository.findByBranchNumber(createReportTO.branchNo());
        User user = userRepository.findById(createReportTO.userId()).orElseThrow(EntityNotFoundException::new);
        report.setUser(user);
        report.setTitle(createReportTO.title());
        report.setType(createReportTO.type());
        report.setBranch(branch);
        report.setFinalPrice(createReportTO.finalPrice());
        report.setCreatedAt(LocalDateTime.now(BAHRAIN));
        return report;
    }

    public List<InventoryProduct> toInventoryProducts(List<CreateReportTO.CreateReportProductsTO> createReportProductsTOS,  Report report) {
        log.info("[Converting products from entity ${} to inventory products]", createReportProductsTOS.toString());
        if(createReportProductsTOS == null) return List.of();

        return createReportProductsTOS.stream().map((inventoryProductTO) -> {
            InventoryProduct inventoryProduct  =  new InventoryProduct();
            try {
                inventoryProduct.setReport(report);
                inventoryProduct.setProduct(productRepository.getReferenceById(inventoryProductTO.id()));
                inventoryProduct.setQuantity(inventoryProductTO.quantity());
                inventoryProduct.setTotalPrice(inventoryProductTO.finalPrice());
                return inventoryProduct;
            }
            catch (EntityNotFoundException e) {
                log.error("[INVENTORY PRODUCTS] product not found");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }
        }).toList();
    }

    public List<InventoryProduct> toInventoryProductsEntity(List<EditReportTO.EditReportProductsTO> editReportProductsTOS, Report report) {
        if(editReportProductsTOS == null) return List.of();

        return editReportProductsTOS.stream().map((inventoryProductTO) -> {
            Product product = productRepository.findById(inventoryProductTO.id()).orElseThrow(() -> new EntityNotFoundException("Product not found"));
            InventoryProduct inventoryProduct  =  new InventoryProduct();
            inventoryProduct.setProduct(product);
            inventoryProduct.setReport(report);
            inventoryProduct.setQuantity(inventoryProductTO.quantity());
            inventoryProduct.setTotalPrice(inventoryProductTO.finalPrice());
            return inventoryProduct;
        }).toList();
    }

    public ReportTO toOrderTO(Report report) {
        List<InventoryProduct> products = inventoryProductRepository.getByReport(report);
        return new ReportTO(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getBranch().getBranchNumber(),
                report.getUser().getId(),
                report.getFinalPrice(),
                toInventoryProductsTO(products)
        );
    }

        public List<ReportTO.InventoryProductsTO>  toInventoryProductsTO(List<InventoryProduct> products) {
        return products.stream().map(product -> {
            ReportTO.InventoryProductsTO inventoryProductsTO = new ReportTO.InventoryProductsTO(
                    toProductTO(product.getProduct()),
                    product.getQuantity(),
                    product.getTotalPrice()
            );
            return inventoryProductsTO;
        }).toList();
    }

    public ProductTO toProductTO(Product product) {
        return new ProductTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getTargetPrice(),
                product.getIsInventory(),
                product.getIsBundle(),
                product.getIsPurchasable(),
                product.getTopVendor()
        );
    }

    public BaseManagementResponse toBaseManagementResponse(Report report) {
        return new BaseManagementResponse(
                report.getId(),
                report.getType(),
                report.getTitle(),
                report.getCreatedAt().toString(),
                report.getBranch().getBranchNumber(),
                report.getUser().getUserName(),
                report.getFinalPrice()
        );
    }

    public ConsumptionReportTO toConsumptionReportTO(Report report, List<ProductConsumptionItem> products) {
        return new ConsumptionReportTO(
                report.getId(),
                report.getTitle(),
                report.getFinalPrice(),
                report.getUser().getId(),
                report.getBranch().getBranchNumber(),
                products
                        .stream()
                        .map((this::toConsumptionProductTO
                )).toList()
        );
    }

    private ConsumptionReportTO.ConsumptionProductTO toConsumptionProductTO(ProductConsumptionItem product) {
         return new ConsumptionReportTO.ConsumptionProductTO(
            product.getProduct().getName(),
                 product.getUsage(),
                 product.getPrice()
        );
    }
}
