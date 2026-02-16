package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name = "toppings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Topping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Boolean available;

    private BigDecimal price;

    private String photo;
}
