package com.icpizza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "jahez_menu")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class JahezMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "jsonb")
    private String json;

    @Column(name = "category_name")
    private String categoryName;
}
