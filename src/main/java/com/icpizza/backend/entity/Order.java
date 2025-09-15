package com.icpizza.backend.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "telephone_no", referencedColumnName = "telephone_no", nullable = true)
    private Customer customer;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "is_ready")
    private Boolean isReady=false;

    @Column(name = "is_paid")
    private Boolean isPaid=false;

    @Column(name = "is_picked_up")
    private Boolean isPickedUp=false;

    @Column(name = "ready_timestamp")
    private Integer readyTimeStamp=null;

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
