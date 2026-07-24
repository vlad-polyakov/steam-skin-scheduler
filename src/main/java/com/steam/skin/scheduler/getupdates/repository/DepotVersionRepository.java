package com.steam.skin.scheduler.getupdates.repository;

import com.steam.skin.scheduler.getupdates.entity.pics.DepotVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepotVersionRepository extends JpaRepository<DepotVersionEntity, Long> {
    Optional<DepotVersionEntity> findFirstByOrderByIdDesc();

    Optional<DepotVersionEntity> findByManifestId(String manifestId);
}
