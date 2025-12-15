package com.icpizza.backend.management.service;

import com.icpizza.backend.management.dto.ProductTO;
import com.icpizza.backend.management.entity.Product;
import com.icpizza.backend.management.entity.PurchaseProduct;
import com.icpizza.backend.management.mapper.ProductMapper;
import com.icpizza.backend.management.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public int overwritePrices(List<PurchaseProduct> purchaseProducts) {
        log.info("[PRODUCT SERVICE] Overwriting prices for products");
        Map<Long, BigDecimal> pricesById = purchaseProducts.stream()
                .filter(purchaseProduct ->
                        purchaseProduct.getPrice() != null && purchaseProduct.getProduct().getId() != null
                )
                .collect(Collectors.toMap(
                        purchaseProduct -> purchaseProduct.getProduct().getId(),
                        PurchaseProduct::getPrice,
                        (oldPrice, newPrice) -> newPrice
                ));
        log.info("Overwriting prices for products, fetched map of products",  pricesById);

        if (pricesById.isEmpty()) return 0;

        List<Product> products = productRepository.findAllById(pricesById.keySet());

        log.info("Fetched products "+products+".");

        List<Product> productsToUpdate = new ArrayList<>(products.size());
        for (Product p : products) {
            BigDecimal newPrice = pricesById.get(p.getId());
            BigDecimal oldPrice = p.getPrice();
            if (newPrice != null && (oldPrice == null || newPrice.compareTo(oldPrice) != 0)) {
                p.setPrice(newPrice);
                productsToUpdate.add(p);
            }
        }

        productRepository.saveAll(productsToUpdate);
        log.info("[PRODUCT SERVICE] Prices for products have been overwritten");
        return productsToUpdate.size();
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
