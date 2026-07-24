package com.steam.skin.scheduler.getupdates.entity.pics;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepotVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manifestId")
    private String manifestId;

    @Column(name = "timestamp")
    private int timestamp;

    @Column(name = "depotKey")
    private byte[] depotKey;
}
