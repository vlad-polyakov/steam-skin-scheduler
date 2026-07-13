package com.steam.skin.scheduler.userauth.entity.steam.auth.common.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteamToken {

    @Id
    @Column(name = "steam_id")
    private Long steamId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "token", length = 1024, nullable = false)
    private String token;

    @Column(name = "expiry_date")
    private Instant expiryDate;
}
