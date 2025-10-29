package com.icpizza.backend.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name = "purchase_products")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private PurchaseReport report;

    private BigDecimal quantity;

    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    private Vendor vendor;

    private BigDecimal finalPrice;
}
