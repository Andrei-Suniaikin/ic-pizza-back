package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "blacklist")
public class BlackListCstmr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="telephoneNo", referencedColumnName = "telephone_no")
    private Customer customer;
}
