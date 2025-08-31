package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String name;
    private String size;
    private BigDecimal price;
    private String photo;
    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "available", nullable = false)
    private boolean available = true;

    @Column(name = "is_best_seller")
    private boolean isBestSeller = false;

    @Column(name = "external_id")
    private String externalId;
}
