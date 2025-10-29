package com.icpizza.backend.management.mapper;

import com.icpizza.backend.management.dto.ProductTO;
import com.icpizza.backend.management.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {
    public List<ProductTO> toProductTO(List<Product> products){
        return products.stream()
                .map(product -> {
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
                }).toList();
    }
}
