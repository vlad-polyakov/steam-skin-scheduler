package com.steam.skin.scheduler.entity.steam.pics;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CS2VersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buildId")
    private String buildId;

    @Column(name = "timestamp")
    private int timestamp;
}
