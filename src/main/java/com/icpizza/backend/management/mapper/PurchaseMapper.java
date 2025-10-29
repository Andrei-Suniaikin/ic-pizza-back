package com.icpizza.backend.management.mapper;

import com.icpizza.backend.entity.User;
import com.icpizza.backend.management.dto.*;
import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.entity.PurchaseProduct;
import com.icpizza.backend.management.entity.PurchaseReport;
import com.icpizza.backend.management.entity.Vendor;
import com.icpizza.backend.management.repository.ProductRepository;
import com.icpizza.backend.management.repository.VendorRepository;
import com.icpizza.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseMapper {
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;

    public List<BasePurchaseResponse> toBasePurchaseResponse(List<PurchaseReport> reports){
        return reports.stream()
                .map(report -> {
                    return new BasePurchaseResponse(
                            report.getId(),
                            report.getTitle(),
                            report.getFinalPrice(),
                            report.getCreatedAt()
                    );
                }).toList();
    }

    public PurchaseReport toPurchaseReportEntity(CreatePurchaseTO purchaseTO) {
        User user = userRepository.findById(purchaseTO.userId()).orElseThrow(() -> new RuntimeException("User not found"));
        PurchaseReport purchaseReport = new PurchaseReport();
        try {
            purchaseReport.setUser(user);
            purchaseReport.setFinalPrice(purchaseTO.finalPrice());
            purchaseReport.setTitle(purchaseTO.title());
            purchaseReport.setUser(user);
            purchaseReport.setCreatedAt(purchaseTO.purchaseDate());
            return purchaseReport;
        }
        catch (Exception e) {
            throw new RuntimeException("Error creating purchase report");
        }
    }

    public List<PurchaseProduct> toPurchaseProductsEntity(PurchaseReport purchaseReport, List<CreatePurchaseTO.PurchaseProductsTO> productsTO) {
        return productsTO.stream()
                .map(productTO -> {
                    Vendor vendor = vendorRepository.findByVendorName(productTO.vendorName());
                    Product product = productRepository.findById(productTO.id()).orElseThrow(() -> new RuntimeException("Product not found"));
                    PurchaseProduct purchaseProduct = new PurchaseProduct();
                    try {
                        purchaseProduct.setProduct(product);
                        purchaseProduct.setVendor(vendor);
                        purchaseProduct.setReport(purchaseReport);
                        purchaseProduct.setQuantity(productTO.quantity());
                        purchaseProduct.setPrice(productTO.price());
                        purchaseProduct.setFinalPrice(productTO.finalPrice());
                        return purchaseProduct;
                    }
                    catch (Exception e) {throw  new RuntimeException("Failed to map product");}
                }).toList();
    }

    public BasePurchaseResponse toBasePurchaseResponse(PurchaseReport purchaseReport) {
        return new BasePurchaseResponse(
                purchaseReport.getId(),
                purchaseReport.getTitle(),
                purchaseReport.getFinalPrice(),
                purchaseReport.getCreatedAt()
        );
    }

    public PurchaseTO toPurchaseTO(PurchaseReport purchaseReport, List<PurchaseProduct> purchaseProducts) {
        return new PurchaseTO(
                purchaseReport.getId(),
                purchaseReport.getTitle(),
                purchaseReport.getFinalPrice(),
                purchaseReport.getUser().getId(),
                purchaseReport.getCreatedAt(),
                toPurchaseProductTO(purchaseProducts)
        );
    }

    public List<PurchaseTO.PurchaseProductsTO> toPurchaseProductTO(List<PurchaseProduct> purchaseProducts) {
        return purchaseProducts
                .stream()
                .map(purchaseProduct ->  {
                        Product product = productRepository.findById(purchaseProduct.getProduct().getId())
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                        return new PurchaseTO.PurchaseProductsTO(
                                toProductTO(product),
                                purchaseProduct.getQuantity(),
                                purchaseProduct.getFinalPrice(),
                                purchaseProduct.getPrice(),
                                purchaseProduct.getVendor().getVendorName()
                        );
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

    public List<PurchaseProduct> toPurchaseProductsEntity(List<EditPurchaseTO.PurchaseProductsTO> purchaseProductsTOS, PurchaseReport purchaseReport) {
        return purchaseProductsTOS.stream()
                .map(productTO -> {
                    Vendor vendor = vendorRepository.findByVendorName(productTO.vendorName());
                    Product product = productRepository.findById(productTO.id()).orElseThrow(() -> new RuntimeException("Product not found"));
                    PurchaseProduct purchaseProduct = new PurchaseProduct();
                    try {
                        purchaseProduct.setProduct(product);
                        purchaseProduct.setVendor(vendor);
                        purchaseProduct.setReport(purchaseReport);
                        purchaseProduct.setQuantity(productTO.quantity());
                        purchaseProduct.setPrice(productTO.price());
                        purchaseProduct.setFinalPrice(productTO.finalPrice());
                        return purchaseProduct;
                    }
                    catch (Exception e) {throw  new RuntimeException("Failed to map product");}
                }).toList();
    }
}
