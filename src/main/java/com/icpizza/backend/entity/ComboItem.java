package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "combo_items")
public class ComboItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    private String category;

    private String name;

    private String size;

    @Column(name = "is_garlic_crust")
    private boolean isGarlicCrust;

    @Column(name = "is_thin_dough")
    private boolean isThinDough;

    private String description;

    private Integer quantity;
}
