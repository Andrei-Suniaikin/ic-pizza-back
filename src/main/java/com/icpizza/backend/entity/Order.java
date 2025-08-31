package com.icpizza.backend.entity;


import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private Integer orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telephone_no", referencedColumnName = "telephone_no")
    private Customer customer;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "amount_paid", precision = 38, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "payment_type", length = 255)
    private String paymentType;

    @Column(name = "external_id")
    private Long externalId;
}
