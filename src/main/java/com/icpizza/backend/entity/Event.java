package com.icpizza.backend.entity;

import com.icpizza.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Event {
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private LocalDateTime datetime;

    @Column(name = "prep_plan", columnDefinition = "TEXT")
    private String prepPlan;

    @Column(name = "cash_amount")
    private BigDecimal cashAmount;

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @Column(name = "shift_no", nullable = false)
    private Integer shiftNo;
}
