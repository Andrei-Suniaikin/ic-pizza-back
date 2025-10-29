package com.icpizza.backend.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(precision = 19, scale = 3)
    private BigDecimal price;

    @Column(name="target_price", precision = 19, scale = 3)
    private BigDecimal targetPrice;

    @Column(name = "is_inventory")
    private Boolean isInventory;

    @Column(name = "is_purchasable")
    private Boolean isPurchasable;

    @Column(name = "is_bundle")
    private Boolean isBundle;

    @Column(name = "top_vendor")
    private String topVendor;
}
