package com.icpizza.backend.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name="product_consumption_items")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductConsumptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private Report report;

    @Column(precision = 19, scale = 3)
    private BigDecimal usage;

    @Column(precision = 19, scale = 3)
    private BigDecimal price;
}
