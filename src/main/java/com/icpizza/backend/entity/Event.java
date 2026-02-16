package com.icpizza.backend.entity;

import com.icpizza.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private LocalDateTime datetime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cash_amount")
    private BigDecimal cashAmount;

    @JoinColumn(name = "branch_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Branch branch;

    @Column(name = "shift_no", nullable = true)
    private Integer shiftNo;
}
