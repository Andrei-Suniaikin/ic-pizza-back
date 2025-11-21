package com.icpizza.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Customer {
    @Id
    @Column(name = "telephone_no", length = 255, nullable = false, unique = true)
    private String telephoneNo;

    @Column(name = "id", nullable = false, length = 255, unique = true)
    private String id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "amount_of_orders")
    private Integer amountOfOrders = 0;

    @Column(name = "amount_paid", precision = 38, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "last_order", length = 255)
    private String lastOrder;

    @Column(name = "waiting_for_name")
    private Integer waitingForName;
}

