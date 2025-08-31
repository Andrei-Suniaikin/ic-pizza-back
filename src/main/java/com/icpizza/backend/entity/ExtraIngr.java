package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "extra_ingr")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ExtraIngr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "photo", length = 255)
    private String photo;

    @Column(name = "price", precision = 38, scale = 2)
    private BigDecimal price;

    @Column(name = "size", length = 255)
    private String size;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "external_id", length = 255)
    private String externalId;
}
