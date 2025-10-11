package com.icpizza.backend.entity;

import com.icpizza.backend.enums.WorkLoadLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Table(name = "branches")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    String address;

    @Column(name = "branch_number", nullable = false)
    int branchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "work_load_level")
    WorkLoadLevel workLoadLevel = WorkLoadLevel.IDLE;

    @Column(name="external_id")
    private String externalId;
}
