package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private String name;
    private Integer quantity;
    private BigDecimal amount;
    private String size;
    private String category;

    @Column(name = "is_garlic_crust")
    private boolean isGarlicCrust;

    @Column(name = "is_thin_dough")
    private boolean isThinDough;

    private String description;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;
}
