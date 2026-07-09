package com.steam.skin.scheduler.repository;

import com.steam.skin.scheduler.entity.steam.pics.DepotVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepotVersionRepository extends JpaRepository<DepotVersionEntity, Long> {
    Optional<DepotVersionEntity> findFirstByOrderByIdDesc();
}
