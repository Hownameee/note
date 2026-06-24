package com.example.mappingdemo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "demo_table")
@Data
public class DemoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Maps to standard 4-byte INT in Postgres
    @Column(name = "int_col")
    private Integer intCol;

    // Maps to standard 8-byte BIGINT in Postgres
    @Column(name = "bigint_col")
    private Long bigintCol;
}
