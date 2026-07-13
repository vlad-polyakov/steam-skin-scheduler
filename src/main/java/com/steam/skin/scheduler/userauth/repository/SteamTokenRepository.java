package com.steam.skin.scheduler.userauth.repository;

import com.steam.skin.scheduler.userauth.entity.steam.auth.common.token.SteamToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SteamTokenRepository extends JpaRepository<SteamToken, Long> {


    Optional<SteamToken> findBySteamId(Long steamId);

    Optional<SteamToken> findByUsername(String username);

    void deleteByExpiryDateBefore(java.time.Instant now);
}
