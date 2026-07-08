package com.steam.skin.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Builder
public class Skin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "collectionId")
    private Long collectionId;
    @Column(name = "name")
    private String name;
    @Column(name = "rarity")
    private Long rarity;
    @Column(name = "minFloat")
    private BigDecimal minFloat;
    @Column(name = "maxFloat")
    private BigDecimal maxFloat;
    @Column(name = "startrek")
    private Boolean startrek;
}
