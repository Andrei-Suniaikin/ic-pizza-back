package com.icpizza.backend.management.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name="shifts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Report report;

    private LocalDate shiftDate;
    @Column(nullable = true)
    private LocalTime cookStartShift;
    @Column(nullable = true)
    private LocalTime cookEndShift;
    @Column(nullable = true)
    private Double cookTotalHours;
    @Column(nullable = true)
    private LocalTime managerStartShift;
    @Column(nullable = true)
    private LocalTime managerEndShift;
    @Column(nullable = true)
    private Double managerTotalHours;
}
