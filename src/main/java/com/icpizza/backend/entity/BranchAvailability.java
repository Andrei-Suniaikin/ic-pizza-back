package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "branch_availability")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BranchAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="branch_id",  nullable=false)
    Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="menu_item_id",  nullable=false)
    MenuItem item;

    @Column(name = "is_available")
    Boolean isAvailable;
}
